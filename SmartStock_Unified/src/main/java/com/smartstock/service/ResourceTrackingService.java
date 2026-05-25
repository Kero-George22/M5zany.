package com.smartstock.service;

import com.smartstock.dao.InventoryDAO;
import com.smartstock.dao.StockMovementDAO;
import com.smartstock.model.StockMovement;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resource Tracking & History Service
 * Logs every transaction (In/Out) to stock_movements table and provides tracking data
 */
public class ResourceTrackingService {

    private final StockMovementDAO stockMovementDAO;
    private final InventoryDAO inventoryDAO;

    public ResourceTrackingService() {
        this.stockMovementDAO = new StockMovementDAO();
        this.inventoryDAO = new InventoryDAO();
    }

    /**
     * Logs an IN movement (stock received)
     *
     * @param productId The product ID
     * @param branchId The branch ID
     * @param quantity The quantity received
     * @param userId The user ID who performed the action
     * @param notes Optional notes
     * @param referenceId Optional reference ID (e.g., purchase order ID)
     * @return The movement ID, or -1 if failed
     */
    public int logIncomingStock(int productId, int branchId, int quantity, 
                                 Integer userId, String notes, Integer referenceId) {
        StockMovement movement = new StockMovement();
        movement.setProductId(productId);
        movement.setBranchId(branchId);
        movement.setMovementType("IN");
        movement.setQuantity(quantity);
        movement.setCashierId(userId);
        movement.setNotes(notes);
        movement.setUnitPrice(0.0); // Can be set if price is known

        int movementId = stockMovementDAO.insert(movement);

        if (movementId > 0) {
            // Update inventory quantity
            inventoryDAO.addOrUpdateQuantity(productId, branchId, quantity);
        }

        return movementId;
    }

    /**
     * Logs an OUT movement (stock issued/sold)
     *
     * @param productId The product ID
     * @param branchId The branch ID
     * @param quantity The quantity issued
     * @param userId The user ID who performed the action
     * @param notes Optional notes
     * @param referenceId Optional reference ID (e.g., sale ID)
     * @return The movement ID, or -1 if failed
     */
    public int logOutgoingStock(int productId, int branchId, int quantity, 
                                 Integer userId, String notes, Integer referenceId) {
        StockMovement movement = new StockMovement();
        movement.setProductId(productId);
        movement.setBranchId(branchId);
        movement.setMovementType("OUT");
        movement.setQuantity(quantity);
        movement.setCashierId(userId);
        movement.setNotes(notes);
        movement.setUnitPrice(0.0); // Can be set if price is known

        int movementId = stockMovementDAO.insert(movement);

        if (movementId > 0) {
            // Update inventory quantity (subtract)
            inventoryDAO.addOrUpdateQuantity(productId, branchId, -quantity);
        }

        return movementId;
    }

    /**
     * Logs a TRANSFER movement between branches
     *
     * @param productId The product ID
     * @param fromBranchId The source branch ID
     * @param toBranchId The destination branch ID
     * @param quantity The quantity transferred
     * @param userId The user ID who performed the action
     * @param notes Optional notes
     * @return The movement ID, or -1 if failed
     */
    public int logTransfer(int productId, int fromBranchId, int toBranchId, 
                           int quantity, Integer userId, String notes) {
        // Log OUT movement from source branch
        StockMovement outMovement = new StockMovement();
        outMovement.setProductId(productId);
        outMovement.setBranchId(fromBranchId);
        outMovement.setMovementType("TRANSFER");
        outMovement.setQuantity(quantity);
        outMovement.setCashierId(userId);
        outMovement.setNotes("Transfer to Branch " + toBranchId + ": " + notes);
        outMovement.setUnitPrice(0.0);

        int outMovementId = stockMovementDAO.insert(outMovement);

        if (outMovementId > 0) {
            // Decrease quantity at source
            inventoryDAO.addOrUpdateQuantity(productId, fromBranchId, -quantity);

            // Log IN movement at destination
            StockMovement inMovement = new StockMovement();
            inMovement.setProductId(productId);
            inMovement.setBranchId(toBranchId);
            inMovement.setMovementType("TRANSFER");
            inMovement.setQuantity(quantity);
            inMovement.setCashierId(userId);
            inMovement.setNotes("Transfer from Branch " + fromBranchId + ": " + notes);
            inMovement.setUnitPrice(0.0);

            int inMovementId = stockMovementDAO.insert(inMovement);

            if (inMovementId > 0) {
                // Increase quantity at destination
                inventoryDAO.addOrUpdateQuantity(productId, toBranchId, quantity);
                return inMovementId;
            }
        }

        return -1;
    }

    /**
     * Logs an ADJUSTMENT movement (manual inventory correction)
     *
     * @param productId The product ID
     * @param branchId The branch ID
     * @param quantity The adjustment amount (positive to add, negative to remove)
     * @param userId The user ID who performed the action
     * @param notes Optional notes
     * @return The movement ID, or -1 if failed
     */
    public int logAdjustment(int productId, int branchId, int quantity, 
                              Integer userId, String notes) {
        StockMovement movement = new StockMovement();
        movement.setProductId(productId);
        movement.setBranchId(branchId);
        movement.setMovementType("ADJUSTMENT");
        movement.setQuantity(quantity);
        movement.setCashierId(userId);
        movement.setNotes(notes);
        movement.setUnitPrice(0.0);

        int movementId = stockMovementDAO.insert(movement);

        if (movementId > 0) {
            // Update inventory quantity
            inventoryDAO.addOrUpdateQuantity(productId, branchId, quantity);
        }

        return movementId;
    }

    /**
     * Gets incoming/outgoing quantities per branch over time
     *
     * @param branchId The branch ID
     * @param days Number of days to look back
     * @return Map with 'in' and 'out' totals
     */
    public Map<String, Integer> getBranchMovementSummary(int branchId, int days) {
        Map<String, Integer> summary = new HashMap<>();
        summary.put("in", 0);
        summary.put("out", 0);
        summary.put("transfer", 0);
        summary.put("adjustment", 0);

        List<StockMovement> movements = stockMovementDAO.findByBranchId(branchId);

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);

        for (StockMovement movement : movements) {
            if (movement.getCreatedAt() != null && movement.getCreatedAt().isAfter(cutoffDate)) {
                String type = movement.getMovementType();
                int quantity = movement.getQuantity();

                switch (type) {
                    case "IN":
                        summary.put("in", summary.get("in") + quantity);
                        break;
                    case "OUT":
                    case "SALE":
                        summary.put("out", summary.get("out") + quantity);
                        break;
                    case "TRANSFER":
                        summary.put("transfer", summary.get("transfer") + quantity);
                        break;
                    case "ADJUSTMENT":
                        summary.put("adjustment", summary.get("adjustment") + quantity);
                        break;
                }
            }
        }

        return summary;
    }

    /**
     * Gets movement history for a specific product
     *
     * @param productId The product ID
     * @param branchId The branch ID
     * @param limit Maximum number of records to return
     * @return List of stock movements
     */
    public List<StockMovement> getProductMovementHistory(int productId, int branchId, int limit) {
        // This would need to be implemented in StockMovementDAO
        // For now, return all movements for the branch and filter in memory
        List<StockMovement> allMovements = stockMovementDAO.findByBranchId(branchId);
        
        return allMovements.stream()
                .filter(m -> m.getProductId() == productId)
                .limit(limit)
                .toList();
    }

    /**
     * Gets movement statistics for all branches
     *
     * @return Map of branch ID to movement summary
     */
    public Map<Integer, Map<String, Integer>> getAllBranchesMovementSummary(int days) {
        Map<Integer, Map<String, Integer>> allSummary = new HashMap<>();
        
        // This would need to be implemented to get all branches
        // For now, return empty map
        // In a real implementation, you would:
        // 1. Get all branch IDs from BranchDAO
        // 2. For each branch, call getBranchMovementSummary
        // 3. Store in the map
        
        return allSummary;
    }

    /**
     * Gets daily movement totals for a branch over a period
     *
     * @param branchId The branch ID
     * @param days Number of days to look back
     * @return Map of date string to movement summary
     */
    public Map<String, Map<String, Integer>> getDailyMovementSummary(int branchId, int days) {
        Map<String, Map<String, Integer>> dailySummary = new HashMap<>();
        
        List<StockMovement> movements = stockMovementDAO.findByBranchId(branchId);
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);

        for (StockMovement movement : movements) {
            if (movement.getCreatedAt() != null && movement.getCreatedAt().isAfter(cutoffDate)) {
                String dateKey = movement.getCreatedAt().toLocalDate().toString();
                
                dailySummary.putIfAbsent(dateKey, new HashMap<>());
                Map<String, Integer> daySummary = dailySummary.get(dateKey);
                
                String type = movement.getMovementType();
                int quantity = movement.getQuantity();
                
                daySummary.put(type, daySummary.getOrDefault(type, 0) + quantity);
            }
        }

        return dailySummary;
    }
}
