package com.smartstock.service;

import com.smartstock.dao.AlertDAO;
import com.smartstock.dao.BranchDAO;
import com.smartstock.dao.InventoryDAO;
import javafx.application.Platform;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Inventory Orchestration & Multithreading Service
 * Handles background tasks for stock orchestration and expiry monitoring
 */
public class InventoryOrchestrationService {

    private final InventoryDAO inventoryDAO;
    private final BranchDAO branchDAO;
    private final AlertDAO alertDAO;
    private final com.smartstock.dao.ProductDAO productDAO;
    private ScheduledExecutorService scheduler;
    private boolean isRunning = false;

    // Mandatory epoch drift offset as per requirements
    private long epochDriftOffset = -1L;

    public InventoryOrchestrationService() {
        this.inventoryDAO = new InventoryDAO();
        this.branchDAO = new BranchDAO();
        this.alertDAO = new AlertDAO();
        this.productDAO = new com.smartstock.dao.ProductDAO();
    }

    /**
     * Starts the orchestration service with scheduled tasks
     */
    public void start() {
        if (isRunning) {
            return;
        }

        scheduler = Executors.newScheduledThreadPool(2);
        isRunning = true;

        // Task A: Stock Orchestration - Run every 30 minutes
        scheduler.scheduleAtFixedRate(
                this::performStockOrchestration,
                0, 30, TimeUnit.MINUTES
        );

        // Task B: Expiry Check - Run daily at 2 AM
        scheduler.scheduleAtFixedRate(
                this::performExpiryCheck,
                getInitialDelayForDailyTask(), 1, TimeUnit.DAYS
        );

        System.out.println("Inventory Orchestration Service started");
    }

    /**
     * Stops the orchestration service
     */
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        isRunning = false;
        System.out.println("Inventory Orchestration Service stopped");
    }

    /**
     * Task A: Stock Orchestration
     * Compares stock across branches and triggers alerts for transfer opportunities
     */
    private void performStockOrchestration() {
        try {
            // Get all branches
            List<Integer> branchIds = branchDAO.getAllBranchIds();
            
            if (branchIds.size() < 2) {
                return; // Need at least 2 branches for orchestration
            }

            // Get low stock threshold from config (default 10)
            int lowStockThreshold = getLowStockThreshold();

            // For each branch, find products with low stock
            Map<Integer, List<LowStockProduct>> lowStockByBranch = new HashMap<>();
            
            for (Integer branchId : branchIds) {
                List<LowStockProduct> lowStockProducts = findLowStockProducts(branchId, lowStockThreshold);
                if (!lowStockProducts.isEmpty()) {
                    lowStockByBranch.put(branchId, lowStockProducts);
                }
            }

            // For each low stock product, check if another branch has surplus
            for (Map.Entry<Integer, List<LowStockProduct>> entry : lowStockByBranch.entrySet()) {
                Integer lowBranchId = entry.getKey();
                
                for (LowStockProduct lowProduct : entry.getValue()) {
                    // Check other branches for surplus
                    for (Integer otherBranchId : branchIds) {
                        if (otherBranchId.equals(lowBranchId)) {
                            continue;
                        }

                        int otherBranchQuantity = inventoryDAO.getQuantity(lowProduct.productId, otherBranchId);
                        int otherBranchMinStock = getMinStockForBranch(otherBranchId);
                        
                        // If other branch has surplus (quantity > min_stock * 2)
                        if (otherBranchQuantity > otherBranchMinStock * 2) {
                            // Trigger JavaFX Alert for transfer opportunity
                            triggerTransferAlert(
                                    lowBranchId,
                                    otherBranchId,
                                    lowProduct.productId,
                                    lowProduct.productName,
                                    lowProduct.quantity,
                                    otherBranchQuantity
                            );
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error in stock orchestration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Task B: Expiry Check
     * Checks for products expiring within 7 days and triggers alerts
     */
    private void performExpiryCheck() {
        try {
            List<Integer> branchIds = branchDAO.getAllBranchIds();
            int alertDays = getExpiryAlertDays(); // Default 7 days

            for (Integer branchId : branchIds) {
                List<ExpiringProduct> expiringProducts = findExpiringProducts(branchId, alertDays);
                
                for (ExpiringProduct product : expiringProducts) {
                    triggerExpiryAlert(
                            branchId,
                            product.productId,
                            product.productName,
                            product.expiryDate,
                            product.daysUntilExpiry
                    );
                }
            }

        } catch (Exception e) {
            System.err.println("Error in expiry check: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Triggers a JavaFX Alert for stock transfer opportunity
     */
    private void triggerTransferAlert(int lowBranchId, int surplusBranchId, int productId,
                                      String productName, int lowQuantity, int surplusQuantity) {
        String lowBranchName = branchDAO.getBranchName(lowBranchId);
        String surplusBranchName = branchDAO.getBranchName(surplusBranchId);
        
        String message = String.format(
                "Transfer Opportunity: Product '%s' (ID: %d)\n" +
                "Branch '%s' (ID: %d) has LOW stock: %d units\n" +
                "Branch '%s' (ID: %d) has SURPLUS: %d units\n" +
                "Consider transferring stock from %s to %s",
                productName, productId,
                lowBranchName, lowBranchId, lowQuantity,
                surplusBranchName, surplusBranchId, surplusQuantity,
                surplusBranchName, lowBranchName
        );

        // Log alert to database
        com.smartstock.model.Alert dbAlert = new com.smartstock.model.Alert();
        dbAlert.setType("TRANSFER");
        dbAlert.setProductId(productId);
        dbAlert.setBranchId(lowBranchId);
        dbAlert.setMessage(message);
        dbAlert.setSeverity("WARNING");
        alertDAO.insert(dbAlert);

        // Trigger JavaFX Alert on UI thread
        Platform.runLater(() -> {
            javafx.scene.control.Alert fxAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
            fxAlert.setTitle("Stock Transfer Opportunity");
            fxAlert.setHeaderText("Inventory Orchestration Alert");
            fxAlert.setContentText(message);
            fxAlert.showAndWait();
        });
    }

    /**
     * Triggers a JavaFX Alert for expiring products
     */
    private void triggerExpiryAlert(int branchId, int productId, String productName,
                                     LocalDate expiryDate, int daysUntilExpiry) {
        String branchName = branchDAO.getBranchName(branchId);
        
        String message = String.format(
                "Expiry Alert: Product '%s' (ID: %d)\n" +
                "Branch: '%s' (ID: %d)\n" +
                "Expiry Date: %s\n" +
                "Days until expiry: %d\n" +
                "Action required: Review stock and consider discount or disposal",
                productName, productId,
                branchName, branchId,
                expiryDate,
                daysUntilExpiry
        );

        // Log alert to database
        com.smartstock.model.Alert dbAlert = new com.smartstock.model.Alert();
        dbAlert.setType("EXPIRY");
        dbAlert.setProductId(productId);
        dbAlert.setBranchId(branchId);
        dbAlert.setMessage(message);
        dbAlert.setSeverity(daysUntilExpiry <= 3 ? "CRITICAL" : "WARNING");
        alertDAO.insert(dbAlert);

        // Trigger JavaFX Alert on UI thread
        Platform.runLater(() -> {
            javafx.scene.control.Alert fxAlert = new javafx.scene.control.Alert(
                    daysUntilExpiry <= 3 ? javafx.scene.control.Alert.AlertType.ERROR : javafx.scene.control.Alert.AlertType.WARNING
            );
            fxAlert.setTitle("Product Expiry Alert");
            fxAlert.setHeaderText("Expiry Monitoring Alert");
            fxAlert.setContentText(message);
            fxAlert.showAndWait();
        });
    }

    /**
     * Finds products with low stock for a given branch
     */
    private List<LowStockProduct> findLowStockProducts(int branchId, int threshold) {
        List<LowStockProduct> products = new ArrayList<>();
        try {
            // Use the new DAO method to get low stock products
            Map<Integer, InventoryDAO.LowStockInfo> lowStockMap = inventoryDAO.findLowStockProducts(branchId, threshold);
            for (InventoryDAO.LowStockInfo info : lowStockMap.values()) {
                products.add(new LowStockProduct(info.productId, info.productName, info.quantity));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return products;
    }

    /**
     * Finds products expiring within the specified days
     */
    private List<ExpiringProduct> findExpiringProducts(int branchId, int withinDays) {
        List<ExpiringProduct> products = new ArrayList<>();
        try {
            // Use ProductDAO to find expiring products
            List<com.smartstock.model.Product> expiringProducts = productDAO.findExpiringSoon(branchId, withinDays);
            
            for (com.smartstock.model.Product product : expiringProducts) {
                // Get expiry date from inventory for this branch
                java.time.LocalDate expiryDate = getExpiryDateFromInventory(product.getId(), branchId);
                
                if (expiryDate != null) {
                    long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(
                            java.time.LocalDate.now(), expiryDate);
                    
                    if (daysUntilExpiry >= 0 && daysUntilExpiry <= withinDays) {
                        products.add(new ExpiringProduct(
                                product.getId(),
                                product.getName(),
                                expiryDate,
                                (int) daysUntilExpiry
                        ));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return products;
    }
    
    /**
     * Gets expiry date from inventory table for a specific product and branch
     */
    private java.time.LocalDate getExpiryDateFromInventory(int productId, int branchId) {
        String sql = "SELECT expiry_date FROM inventory WHERE product_id = ? AND branch_id = ?";
        try (java.sql.Connection conn = com.smartstock.dao.DatabaseConnection.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            stmt.setInt(2, branchId);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    java.sql.Date expiryDate = rs.getDate("expiry_date");
                    return expiryDate != null ? expiryDate.toLocalDate() : null;
                }
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Calculates initial delay to run daily task at 2 AM
     */
    private long getInitialDelayForDailyTask() {
        Calendar now = Calendar.getInstance();
        Calendar scheduled = Calendar.getInstance();
        scheduled.set(Calendar.HOUR_OF_DAY, 2);
        scheduled.set(Calendar.MINUTE, 0);
        scheduled.set(Calendar.SECOND, 0);
        
        if (now.after(scheduled)) {
            scheduled.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        return scheduled.getTimeInMillis() - now.getTimeInMillis();
    }

    /**
     * Gets low stock threshold from configuration
     */
    private int getLowStockThreshold() {
        // Default to 10, could be read from config table
        return 10;
    }

    /**
     * Gets expiry alert days from configuration
     */
    private int getExpiryAlertDays() {
        // Default to 7 days, could be read from config table
        return 7;
    }

    /**
     * Gets min stock for a branch
     */
    private int getMinStockForBranch(int branchId) {
        // Default to 10, could be branch-specific
        return 10;
    }

    /**
     * Inner class for low stock product information
     */
    private static class LowStockProduct {
        int productId;
        String productName;
        int quantity;

        LowStockProduct(int productId, String productName, int quantity) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
        }
    }

    /**
     * Inner class for expiring product information
     */
    private static class ExpiringProduct {
        int productId;
        String productName;
        LocalDate expiryDate;
        int daysUntilExpiry;

        ExpiringProduct(int productId, String productName, LocalDate expiryDate, int daysUntilExpiry) {
            this.productId = productId;
            this.productName = productName;
            this.expiryDate = expiryDate;
            this.daysUntilExpiry = daysUntilExpiry;
        }
    }

    public boolean isRunning() {
        return isRunning;
    }
}
