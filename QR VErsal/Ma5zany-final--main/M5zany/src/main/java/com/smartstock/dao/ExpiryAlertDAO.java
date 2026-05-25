package com.smartstock.dao;

import com.smartstock.model.ExpiryAlert;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ExpiryAlertDAO {

    public int insert(ExpiryAlert alert) {
        String sql = "INSERT INTO expiry_alerts (branch_id, product_id, expiry_date, days_remaining, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, alert.getBranchId());
            stmt.setInt(2, alert.getProductId());
            stmt.setDate(3, Date.valueOf(alert.getExpiryDate()));
            if (alert.getDaysRemaining() != null) stmt.setInt(4, alert.getDaysRemaining());
            else stmt.setNull(4, Types.INTEGER);
            stmt.setString(5, alert.getStatus() != null ? alert.getStatus() : "PENDING");
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public List<ExpiryAlert> findByBranch(int branchId) {
        List<ExpiryAlert> list = new ArrayList<>();
        String sql = "SELECT ea.*, p.name as product_name, b.name as branch_name " +
                "FROM expiry_alerts ea " +
                "JOIN products p ON ea.product_id = p.id " +
                "JOIN branches b ON ea.branch_id = b.id " +
                "WHERE ea.branch_id = ? ORDER BY ea.expiry_date ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<ExpiryAlert> findPending() {
        List<ExpiryAlert> list = new ArrayList<>();
        String sql = "SELECT ea.*, p.name as product_name, b.name as branch_name " +
                "FROM expiry_alerts ea " +
                "JOIN products p ON ea.product_id = p.id " +
                "JOIN branches b ON ea.branch_id = b.id " +
                "WHERE ea.status = 'PENDING' ORDER BY ea.days_remaining ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean updateStatus(int alertId, String status) {
        String sql = "UPDATE expiry_alerts SET status=? WHERE alert_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, alertId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    /**
     * Scans inventory for products expiring within the given threshold days
     * and inserts expiry_alert records (skips if already exists for same product+branch+expiry).
     */
    public int generateAlertsForBranch(int branchId, int thresholdDays) {
        String sql = "INSERT IGNORE INTO expiry_alerts (branch_id, product_id, expiry_date, days_remaining, status) " +
                "SELECT i.branch_id, i.product_id, i.expiry_date, " +
                "DATEDIFF(i.expiry_date, CURDATE()) as days_remaining, 'PENDING' " +
                "FROM inventory i " +
                "WHERE i.branch_id = ? AND i.expiry_date IS NOT NULL " +
                "AND DATEDIFF(i.expiry_date, CURDATE()) BETWEEN 0 AND ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            stmt.setInt(2, thresholdDays);
            return stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    private ExpiryAlert mapRow(ResultSet rs) throws SQLException {
        ExpiryAlert a = new ExpiryAlert();
        a.setAlertId(rs.getInt("alert_id"));
        a.setBranchId(rs.getInt("branch_id"));
        a.setProductId(rs.getInt("product_id"));
        Date d = rs.getDate("expiry_date");
        if (d != null) a.setExpiryDate(d.toLocalDate());
        int dr = rs.getInt("days_remaining");
        if (!rs.wasNull()) a.setDaysRemaining(dr);
        a.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) a.setCreatedAt(ts.toLocalDateTime());
        try { a.setProductName(rs.getString("product_name")); } catch (SQLException ignored) {}
        try { a.setBranchName(rs.getString("branch_name")); } catch (SQLException ignored) {}
        return a;
    }
}
