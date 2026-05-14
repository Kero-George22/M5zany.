package com.smartstock.service;

import com.smartstock.dao.CycleCountLogDAO;
import com.smartstock.dao.InventoryDAO;
import com.smartstock.dao.ProductDAO;
import com.smartstock.model.CycleCountLog;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

/**
 * Cycle Counting Module
 * Suggests different sets of products to count each day and logs results to database
 */
public class CycleCountingService {

    private final CycleCountLogDAO cycleCountLogDAO;
    private final InventoryDAO inventoryDAO;
    private final ProductDAO productDAO;
    private final Random random;

    public CycleCountingService() {
        this.cycleCountLogDAO = new CycleCountLogDAO();
        this.inventoryDAO = new InventoryDAO();
        this.productDAO = new ProductDAO();
        this.random = new Random();
    }

    /**
     * Generates a daily set of products to count for a specific branch
     * Uses a rotating algorithm to ensure different products are counted each day
     *
     * @param branchId The branch ID
     * @param countDate The date for the count
     * @param productCount Number of products to suggest (default 20)
     * @return List of product IDs to count
     */
    public List<Integer> generateDailyCountList(int branchId, LocalDate countDate, int productCount) {
        List<Integer> allProductIds = productDAO.getAllProductIds();
        List<Integer> suggestedProducts = new ArrayList<>();

        if (allProductIds.isEmpty()) {
            return suggestedProducts;
        }

        // Get products already counted in the last 30 days to avoid repetition
        List<Integer> recentlyCounted = getRecentlyCountedProducts(branchId, countDate.minusDays(30));

        // Filter out recently counted products
        List<Integer> availableProducts = new ArrayList<>();
        for (Integer productId : allProductIds) {
            if (!recentlyCounted.contains(productId)) {
                availableProducts.add(productId);
            }
        }

        // If not enough available products, include some recently counted ones
        if (availableProducts.size() < productCount) {
            availableProducts.addAll(allProductIds);
            // Remove duplicates
            availableProducts = new ArrayList<>(new HashSet<>(availableProducts));
        }

        // Prioritize products with low stock or high variance history
        List<Integer> prioritizedProducts = prioritizeProducts(branchId, availableProducts);

        // Select the requested number of products
        int selectionCount = Math.min(productCount, prioritizedProducts.size());
        
        // Use day of year as seed for consistent daily selection
        int dayOfYear = countDate.getDayOfYear();
        random.setSeed(dayOfYear + branchId * 1000);
        
        Collections.shuffle(prioritizedProducts, random);
        
        suggestedProducts = prioritizedProducts.subList(0, selectionCount);
        Collections.sort(suggestedProducts); // Sort for consistency

        return suggestedProducts;
    }

    /**
     * Prioritizes products based on various factors:
     * - Low stock items
     * - High value items
     * - Fast-moving items
     * - Items with previous discrepancies
     */
    private List<Integer> prioritizeProducts(int branchId, List<Integer> productIds) {
        Map<Integer, Integer> priorityScores = new HashMap<>();

        for (Integer productId : productIds) {
            int score = 0;

            // Factor 1: Low stock (higher priority)
            int quantity = inventoryDAO.getQuantity(productId, branchId);
            if (quantity < 10) {
                score += 50;
            } else if (quantity < 20) {
                score += 30;
            } else if (quantity < 50) {
                score += 10;
            }

            // Factor 2: Previous discrepancies (higher priority)
            int discrepancyCount = getDiscrepancyCount(branchId, productId);
            score += discrepancyCount * 20;

            // Factor 3: Random factor to ensure variety
            score += random.nextInt(10);

            priorityScores.put(productId, score);
        }

        // Sort by priority score (descending)
        List<Integer> prioritized = new ArrayList<>(productIds);
        prioritized.sort((a, b) -> priorityScores.get(b).compareTo(priorityScores.get(a)));

        return prioritized;
    }

    /**
     * Gets products that were counted in the recent period
     */
    private List<Integer> getRecentlyCountedProducts(int branchId, LocalDate sinceDate) {
        List<Integer> recentlyCounted = new ArrayList<>();
        List<CycleCountLog> recentLogs = cycleCountLogDAO.findByBranch(branchId);

        for (CycleCountLog log : recentLogs) {
            if (log.getCountDate() != null && !log.getCountDate().isBefore(sinceDate)) {
                recentlyCounted.add(log.getProductId());
            }
        }

        return recentlyCounted;
    }

    /**
     * Gets the count of discrepancies for a product in recent cycle counts
     */
    private int getDiscrepancyCount(int branchId, int productId) {
        int count = 0;
        List<CycleCountLog> logs = cycleCountLogDAO.findByBranch(branchId);

        for (CycleCountLog log : logs) {
            if (log.getProductId() == productId && log.getDiscrepancy() != null && log.getDiscrepancy() != 0) {
                count++;
            }
        }

        return count;
    }

    /**
     * Logs a cycle count result to the database
     *
     * @param branchId The branch ID
     * @param productId The product ID
     * @param countDate The date of the count
     * @param expectedQty The expected quantity from inventory
     * @param countedQty The actual counted quantity
     * @param countedBy The user ID who performed the count
     * @param notes Optional notes
     * @return The count_id of the inserted record, or -1 if failed
     */
    public int logCycleCount(int branchId, int productId, LocalDate countDate,
                             Integer expectedQty, Integer countedQty, Integer countedBy, String notes) {
        CycleCountLog log = new CycleCountLog();
        log.setBranchId(branchId);
        log.setProductId(productId);
        log.setCountDate(countDate);
        log.setExpectedQty(expectedQty);
        log.setCountedQty(countedQty);
        log.setCountedBy(countedBy);
        log.setNotes(notes);

        // Calculate discrepancy (counted - expected)
        if (expectedQty != null && countedQty != null) {
            log.setDiscrepancy(countedQty - expectedQty);
        }

        return cycleCountLogDAO.insert(log);
    }

    /**
     * Gets cycle count statistics for a branch
     */
    public Map<String, Object> getCycleCountStatistics(int branchId) {
        Map<String, Object> stats = new HashMap<>();
        
        List<CycleCountLog> allLogs = cycleCountLogDAO.findByBranch(branchId);
        List<CycleCountLog> discrepancies = cycleCountLogDAO.findDiscrepancies(branchId);

        stats.put("totalCounts", allLogs.size());
        stats.put("discrepancyCount", discrepancies.size());
        stats.put("accuracyRate", calculateAccuracyRate(allLogs));
        stats.put("lastCountDate", getLastCountDate(allLogs));

        return stats;
    }

    /**
     * Calculates the accuracy rate of cycle counts
     */
    private double calculateAccuracyRate(List<CycleCountLog> logs) {
        if (logs.isEmpty()) {
            return 0.0;
        }

        int accurateCounts = 0;
        for (CycleCountLog log : logs) {
            if (log.getDiscrepancy() == null || log.getDiscrepancy() == 0) {
                accurateCounts++;
            }
        }

        return (double) accurateCounts / logs.size() * 100;
    }

    /**
     * Gets the date of the most recent cycle count
     */
    private LocalDate getLastCountDate(List<CycleCountLog> logs) {
        if (logs.isEmpty()) {
            return null;
        }

        LocalDate lastDate = null;
        for (CycleCountLog log : logs) {
            if (log.getCountDate() != null && (lastDate == null || log.getCountDate().isAfter(lastDate))) {
                lastDate = log.getCountDate();
            }
        }

        return lastDate;
    }

    /**
     * Generates a weekly cycle count schedule for all branches
     */
    public Map<Integer, List<Integer>> generateWeeklySchedule(LocalDate weekStart) {
        Map<Integer, List<Integer>> schedule = new HashMap<>();
        
        List<Integer> branchIds = getAllBranchIds();
        
        for (Integer branchId : branchIds) {
            for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
                LocalDate countDate = weekStart.plusDays(dayOffset);
                List<Integer> productsForDay = generateDailyCountList(branchId, countDate, 20);
                
                if (!productsForDay.isEmpty()) {
                    int scheduleKey = branchId * 100 + dayOffset; // Unique key for branch+day
                    schedule.put(scheduleKey, productsForDay);
                }
            }
        }

        return schedule;
    }

    /**
     * Gets all branch IDs (placeholder - would use BranchDAO)
     */
    private List<Integer> getAllBranchIds() {
        // This would use BranchDAO to get all branches
        // For now, return a default list
        List<Integer> branches = new ArrayList<>();
        branches.add(1);
        branches.add(2);
        branches.add(3);
        return branches;
    }
}
