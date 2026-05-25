package com.smartstock.service;

import com.smartstock.dao.BranchDAO;
import com.smartstock.dao.ProductDAO;
import com.smartstock.dao.AlertDAO;
import com.smartstock.dao.StockMovementDAO;
import com.smartstock.model.*;

import java.util.List;

public class ReportService {
    private final BranchDAO branchDAO;
    private final ProductDAO productDAO;
    private final AlertDAO alertDAO;
    private final StockMovementDAO stockMovementDAO;

    public ReportService() {
        this.branchDAO = new BranchDAO();
        this.productDAO = new ProductDAO();
        this.alertDAO = new AlertDAO();
        this.stockMovementDAO = new StockMovementDAO();
    }

    public List<Branch> getAllBranchesWithSummary() {
        return branchDAO.findAll();
    }

    public List<Product> getLowStockProducts(int branchId) {
        return productDAO.findLowStock(branchId);
    }

    public List<Product> getFastMovingProducts(int branchId, int threshold) {
        return productDAO.findFastMoving(branchId, threshold);
    }

    public List<Alert> getAllAlerts() {
        return alertDAO.findAll();
    }

    public List<Alert> getUnreadAlerts() {
        return alertDAO.findUnread();
    }

    public List<StockMovement> getRecentMovements(int branchId) {
        return stockMovementDAO.findByBranchId(branchId);
    }

    public void generateExpiryAlerts() {
        String sql = "SELECT p.*, i.quantity FROM products p " +
                "JOIN inventory i ON p.id = i.product_id AND p.branch_id = i.branch_id " +
                "WHERE p.expiry_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 7 DAY)";
        try (java.sql.Connection conn = com.smartstock.dao.DatabaseConnection.getConnection();
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int productId = rs.getInt("id");
                int branchId = rs.getInt("branch_id");
                if (alertDAO.existsUnreadOrRecentSimilar("EXPIRY", productId, branchId)) {
                    continue;
                }
                Alert alert = new Alert();
                alert.setType("EXPIRY");
                alert.setProductId(productId);
                alert.setBranchId(branchId);
                alert.setMessage("Product '" + rs.getString("name") + "' expires on " + rs.getDate("expiry_date"));
                alert.setSeverity("WARNING");
                alertDAO.insert(alert);
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    public void generateLowStockAlerts() {
        String sql = "SELECT p.id, p.name, i.branch_id, i.quantity, i.min_stock FROM products p " +
                "JOIN inventory i ON p.id = i.product_id AND p.branch_id = i.branch_id " +
                "WHERE i.quantity <= i.min_stock";
        try (java.sql.Connection conn = com.smartstock.dao.DatabaseConnection.getConnection();
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int productId = rs.getInt("id");
                int branchId = rs.getInt("branch_id");
                if (alertDAO.existsUnreadOrRecentSimilar("LOW_STOCK", productId, branchId)) {
                    continue;
                }
                Alert alert = new Alert();
                alert.setType("LOW_STOCK");
                alert.setProductId(productId);
                alert.setBranchId(branchId);
                alert.setMessage("Low stock: '" + rs.getString("name") + "' has only " + rs.getInt("quantity") + " units (min: " + rs.getInt("min_stock") + ")");
                alert.setSeverity(rs.getInt("quantity") == 0 ? "CRITICAL" : "WARNING");
                alertDAO.insert(alert);
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    public void generateOutOfStockAlerts() {
        String sql = "SELECT p.id, p.name, i.branch_id FROM products p " +
                "JOIN inventory i ON p.id = i.product_id AND p.branch_id = i.branch_id " +
                "WHERE i.quantity = 0";
        try (java.sql.Connection conn = com.smartstock.dao.DatabaseConnection.getConnection();
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int productId = rs.getInt("id");
                int branchId = rs.getInt("branch_id");
                if (alertDAO.existsUnreadOrRecentSimilar("LOW_STOCK", productId, branchId)) continue;
                Alert alert = new Alert();
                alert.setType("LOW_STOCK");
                alert.setProductId(productId);
                alert.setBranchId(branchId);
                alert.setMessage("Out of stock: '" + rs.getString("name") + "' is completely unavailable in this branch.");
                alert.setSeverity("CRITICAL");
                alertDAO.insert(alert);
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    public void generateNearExpiryAlerts() {
        String sql = "SELECT id, name, branch_id, expiry_date, DATEDIFF(expiry_date, CURDATE()) days_left " +
                "FROM products WHERE expiry_date IS NOT NULL AND DATEDIFF(expiry_date, CURDATE()) BETWEEN 0 AND 30";
        try (java.sql.Connection conn = com.smartstock.dao.DatabaseConnection.getConnection();
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int productId = rs.getInt("id");
                int branchId = rs.getInt("branch_id");
                if (alertDAO.existsUnreadOrRecentSimilar("EXPIRY", productId, branchId)) continue;
                int daysLeft = rs.getInt("days_left");
                if (!(daysLeft == 30 || daysLeft == 14 || daysLeft == 7 || daysLeft == 3 || daysLeft <= 2)) continue;
                Alert alert = new Alert();
                alert.setType("EXPIRY");
                alert.setProductId(productId);
                alert.setBranchId(branchId);
                alert.setMessage("Near expiry: '" + rs.getString("name") + "' expires in " + daysLeft + " day(s).");
                alert.setSeverity(daysLeft <= 3 ? "CRITICAL" : "WARNING");
                alertDAO.insert(alert);
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    public void generateFastDepletionAlerts() {
        String sql = "SELECT p.id, p.name, i.branch_id, i.quantity, COALESCE(s.sold_today,0) sold_today " +
                "FROM products p " +
                "JOIN inventory i ON p.id=i.product_id AND p.branch_id=i.branch_id " +
                "LEFT JOIN (" +
                "  SELECT product_id, branch_id, SUM(quantity) sold_today FROM stock_movements " +
                "  WHERE movement_type='SALE' AND DATE(created_at)=CURDATE() GROUP BY product_id, branch_id" +
                ") s ON s.product_id=p.id AND s.branch_id=i.branch_id";
        try (java.sql.Connection conn = com.smartstock.dao.DatabaseConnection.getConnection();
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int soldToday = rs.getInt("sold_today");
                int currentQty = rs.getInt("quantity");
                int estimatedStart = soldToday + currentQty;
                if (estimatedStart <= 0) continue;
                double depletion = soldToday / (double) estimatedStart;
                if (depletion < 0.40 || soldToday < 5) continue;
                int productId = rs.getInt("id");
                int branchId = rs.getInt("branch_id");
                if (alertDAO.existsUnreadOrRecentSimilar("FAST_MOVING", productId, branchId)) continue;
                Alert alert = new Alert();
                alert.setType("FAST_MOVING");
                alert.setProductId(productId);
                alert.setBranchId(branchId);
                alert.setMessage("Fast depletion: '" + rs.getString("name") + "' consumed " + soldToday + " units today (" + (int)(depletion * 100) + "% of estimated stock).");
                alert.setSeverity("WARNING");
                alertDAO.insert(alert);
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    public void generateCashierAnomalyAlerts() {
        String sql = "SELECT t.transaction_id, t.branch_id, t.final_amount, u.full_name cashier_name, stats.avg_amount " +
                "FROM transactions t " +
                "JOIN users u ON u.id = t.cashier_id " +
                "JOIN (" +
                "  SELECT branch_id, AVG(final_amount) avg_amount FROM transactions WHERE DATE(transaction_at)=CURDATE() GROUP BY branch_id" +
                ") stats ON stats.branch_id=t.branch_id " +
                "WHERE DATE(t.transaction_at)=CURDATE() AND t.final_amount >= GREATEST(stats.avg_amount*2, 500)";
        try (java.sql.Connection conn = com.smartstock.dao.DatabaseConnection.getConnection();
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int branchId = rs.getInt("branch_id");
                int txnId = rs.getInt("transaction_id");
                if (alertDAO.existsUnreadOrRecentSimilar("SYSTEM", txnId, branchId)) continue;
                Alert alert = new Alert();
                alert.setType("SYSTEM");
                alert.setProductId(txnId);
                alert.setBranchId(branchId);
                alert.setMessage("Cashier anomaly: invoice #" + txnId + " by " + rs.getString("cashier_name") +
                        " totals " + rs.getDouble("final_amount") + " (daily branch avg: " + rs.getDouble("avg_amount") + ").");
                alert.setSeverity("WARNING");
                alertDAO.insert(alert);
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    public void generateMultiBranchOutageAlerts() {
        String sql = "SELECT p.id, p.name, COUNT(*) outages " +
                "FROM products p JOIN inventory i ON p.id=i.product_id " +
                "WHERE i.quantity=0 GROUP BY p.id, p.name HAVING COUNT(*) >= 2";
        try (java.sql.Connection conn = com.smartstock.dao.DatabaseConnection.getConnection();
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int productId = rs.getInt("id");
                if (alertDAO.existsUnreadOrRecentSimilar("SYSTEM", productId, null)) continue;
                Alert alert = new Alert();
                alert.setType("SYSTEM");
                alert.setProductId(productId);
                alert.setBranchId(null);
                alert.setMessage("Multi-branch outage: '" + rs.getString("name") + "' is out of stock in " + rs.getInt("outages") + " branches.");
                alert.setSeverity("CRITICAL");
                alertDAO.insert(alert);
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    public void generateOperationalAlerts() {
        generateLowStockAlerts();
        generateOutOfStockAlerts();
        generateNearExpiryAlerts();
        generateFastDepletionAlerts();
        generateCashierAnomalyAlerts();
        generateMultiBranchOutageAlerts();
    }
}
