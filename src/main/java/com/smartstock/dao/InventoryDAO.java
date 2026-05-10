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
}
