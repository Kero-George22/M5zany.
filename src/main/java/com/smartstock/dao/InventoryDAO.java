package com.smartstock.dao;

import java.sql.*;
import java.time.LocalDate;

public class InventoryDAO {

    public boolean updateQuantity(int productId, int branchId, int newQuantity) {
        String sql = "UPDATE inventory SET quantity=? WHERE product_id=? AND branch_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, newQuantity);
            stmt.setInt(2, productId);
            stmt.setInt(3, branchId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean adjustQuantity(int productId, int branchId, int delta) {
        String sql = "UPDATE inventory SET quantity = quantity + ? WHERE product_id=? AND branch_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, delta);
            stmt.setInt(2, productId);
            stmt.setInt(3, branchId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    /**
     * Adjusts quantity; if no inventory record exists for this product+branch, inserts one.
     * Used for stock transfers where destination branch may not have this product yet.
     */
    public boolean addOrUpdateQuantity(int productId, int branchId, int delta) {
        String updateSql = "UPDATE inventory SET quantity = quantity + ? WHERE product_id=? AND branch_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setInt(1, delta);
            stmt.setInt(2, productId);
            stmt.setInt(3, branchId);
            int affected = stmt.executeUpdate();
            if (affected > 0) return true;
            // Row doesn't exist — insert new record
            String insertSql = "INSERT INTO inventory (product_id, branch_id, quantity, min_stock) VALUES (?, ?, ?, 10)";
            try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                ins.setInt(1, productId);
                ins.setInt(2, branchId);
                ins.setInt(3, Math.max(0, delta));
                return ins.executeUpdate() > 0;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public int getQuantity(int productId, int branchId) {
        String sql = "SELECT quantity FROM inventory WHERE product_id=? AND branch_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            stmt.setInt(2, branchId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("quantity");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public int getLowStockCount(int branchId) {
        String sql = "SELECT COUNT(*) FROM inventory WHERE branch_id=? AND quantity < min_stock";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    /**
     * Updates the expiry_date for a specific inventory record (branch + product).
     * New column added in DB migration step 6.
     */
    public boolean updateExpiryDate(int productId, int branchId, LocalDate expiryDate) {
        String sql = "UPDATE inventory SET expiry_date=? WHERE product_id=? AND branch_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (expiryDate != null) stmt.setDate(1, Date.valueOf(expiryDate));
            else stmt.setNull(1, Types.DATE);
            stmt.setInt(2, productId);
            stmt.setInt(3, branchId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    /**
     * Returns the count of inventory records expiring within the given number of days for a branch.
     */
    public int getExpiringCount(int branchId, int withinDays) {
        String sql = "SELECT COUNT(*) FROM inventory WHERE branch_id=? AND expiry_date IS NOT NULL " +
                "AND DATEDIFF(expiry_date, CURDATE()) BETWEEN 0 AND ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            stmt.setInt(2, withinDays);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    /**
     * Gets products with low stock for a branch with product details
     */
    public java.util.Map<Integer, LowStockInfo> findLowStockProducts(int branchId, int threshold) {
        java.util.Map<Integer, LowStockInfo> products = new java.util.HashMap<>();
        String sql = "SELECT i.product_id, i.quantity, p.name as product_name, i.min_stock " +
                "FROM inventory i JOIN products p ON i.product_id = p.id " +
                "WHERE i.branch_id = ? AND i.quantity < ? ORDER BY i.quantity ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            stmt.setInt(2, threshold);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LowStockInfo info = new LowStockInfo();
                    info.productId = rs.getInt("product_id");
                    info.productName = rs.getString("product_name");
                    info.quantity = rs.getInt("quantity");
                    info.minStock = rs.getInt("min_stock");
                    products.put(info.productId, info);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return products;
    }

    /**
     * Gets min stock for a specific branch and product
     */
    public int getMinStock(int productId, int branchId) {
        String sql = "SELECT min_stock FROM inventory WHERE product_id=? AND branch_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            stmt.setInt(2, branchId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("min_stock");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 10; // Default
    }

    /**
     * Inner class for low stock information
     */
    public static class LowStockInfo {
        public int productId;
        public String productName;
        public int quantity;
        public int minStock;
    }
}
