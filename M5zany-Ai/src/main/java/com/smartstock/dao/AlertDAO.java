package com.smartstock.dao;

import com.smartstock.model.Alert;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AlertDAO {
    public int insert(Alert alert) {
        String sql = "INSERT INTO alerts (type, product_id, branch_id, sender_id, message, severity, is_read) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, alert.getType());
            if (alert.getProductId() != null) stmt.setInt(2, alert.getProductId());
            else stmt.setNull(2, Types.INTEGER);
            if (alert.getBranchId() != null) stmt.setInt(3, alert.getBranchId());
            else stmt.setNull(3, Types.INTEGER);
            if (alert.getSenderId() != null) stmt.setInt(4, alert.getSenderId());
            else stmt.setNull(4, Types.INTEGER);
            stmt.setString(5, alert.getMessage());
            stmt.setString(6, alert.getSeverity() != null ? alert.getSeverity() : "INFO");
            stmt.setBoolean(7, alert.isRead());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[AlertDAO.insert] SQL ERROR: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Alert insert failed: " + e.getMessage(), e);
        }
        return -1;
    }

    public List<Alert> findAll() {
        List<Alert> alerts = new ArrayList<>();
        String sql = "SELECT a.*, u.full_name as sender_name FROM alerts a LEFT JOIN users u ON a.sender_id = u.id ORDER BY a.created_at DESC LIMIT 200";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                alerts.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return alerts;
    }

    public List<Alert> findByBranchId(int branchId) {
        List<Alert> alerts = new ArrayList<>();
        String sql = "SELECT a.*, u.full_name as sender_name FROM alerts a LEFT JOIN users u ON a.sender_id = u.id WHERE a.branch_id=? OR a.branch_id IS NULL ORDER BY a.created_at DESC LIMIT 100";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    alerts.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return alerts;
    }

    public List<Alert> findUnread() {
        List<Alert> alerts = new ArrayList<>();
        String sql = "SELECT a.*, u.full_name as sender_name FROM alerts a LEFT JOIN users u ON a.sender_id = u.id WHERE a.is_read=FALSE ORDER BY a.created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                alerts.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return alerts;
    }

    /**
     * Insert a custom alert sent by a manager to admin.
     * branchId can be null for system-wide alerts.
     */
    public boolean insertCustomAlert(int senderId, Integer branchId, String message, String severity) {
        Alert alert = new Alert();
        alert.setType("SYSTEM");  // CUSTOM not in DB ENUM; SYSTEM is the correct fallback
        alert.setSenderId(senderId);
        alert.setBranchId(branchId);
        alert.setMessage(message);
        alert.setSeverity(severity != null ? severity : "INFO");
        int result = insert(alert);
        System.out.println("[AlertDAO] insertCustomAlert result=" + result + " branchId=" + branchId + " senderId=" + senderId);
        return result > 0;
    }

    /**
     * Returns alerts STRICTLY for a specific branch (no global/null-branch alerts).
     * Used for manager's own alert inbox — only sees what was sent to their branch.
     */
    public List<Alert> findForBranch(int branchId) {
        List<Alert> alerts = new ArrayList<>();
        String sql = "SELECT a.*, u.full_name as sender_name FROM alerts a " +
                "LEFT JOIN users u ON a.sender_id = u.id " +
                "WHERE a.branch_id = ? ORDER BY a.created_at DESC LIMIT 100";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) alerts.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return alerts;
    }

    public boolean existsUnreadOrRecentSimilar(String type, Integer productId, Integer branchId) {
        String sql = "SELECT 1 FROM alerts WHERE type = ? AND (is_read = FALSE OR created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)) " +
                "AND ((product_id = ?) OR (product_id IS NULL AND ? IS NULL)) " +
                "AND ((branch_id = ?) OR (branch_id IS NULL AND ? IS NULL)) LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, type);
            if (productId != null) {
                stmt.setInt(2, productId);
                stmt.setInt(3, productId);
            } else {
                stmt.setNull(2, Types.INTEGER);
                stmt.setNull(3, Types.INTEGER);
            }
            if (branchId != null) {
                stmt.setInt(4, branchId);
                stmt.setInt(5, branchId);
            } else {
                stmt.setNull(4, Types.INTEGER);
                stmt.setNull(5, Types.INTEGER);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    public boolean markAsRead(int id) {
        String sql = "UPDATE alerts SET is_read=TRUE WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean markAllRead() {
        String sql = "UPDATE alerts SET is_read=TRUE WHERE is_read=FALSE";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM alerts WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Alert mapRow(ResultSet rs) throws SQLException {
        Alert alert = new Alert();
        alert.setId(rs.getInt("id"));
        alert.setType(rs.getString("type"));
        alert.setProductId(rs.getObject("product_id", Integer.class));
        alert.setBranchId(rs.getObject("branch_id", Integer.class));
        alert.setSenderId(rs.getObject("sender_id", Integer.class));
        try { alert.setSenderName(rs.getString("sender_name")); } catch (SQLException ignored) {}
        alert.setMessage(rs.getString("message"));
        alert.setSeverity(rs.getString("severity"));
        alert.setRead(rs.getBoolean("is_read"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) alert.setCreatedAt(ts.toLocalDateTime());
        return alert;
    }
}
