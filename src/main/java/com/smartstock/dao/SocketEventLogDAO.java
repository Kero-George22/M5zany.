package com.smartstock.dao;

import com.smartstock.model.SocketEventLog;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SocketEventLogDAO {

    public int insert(SocketEventLog log) {
        String sql = "INSERT INTO socket_event_log (branch_id, event_type, payload, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, log.getBranchId());
            stmt.setString(2, log.getEventType());
            stmt.setString(3, log.getPayload());
            stmt.setString(4, log.getStatus() != null ? log.getStatus() : "RECEIVED");
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public boolean updateStatus(int eventId, String status) {
        String sql = "UPDATE socket_event_log SET status=? WHERE event_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, eventId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public List<SocketEventLog> findByBranch(int branchId, int limit) {
        List<SocketEventLog> list = new ArrayList<>();
        String sql = "SELECT * FROM socket_event_log WHERE branch_id=? ORDER BY received_at DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<SocketEventLog> findFailed() {
        List<SocketEventLog> list = new ArrayList<>();
        String sql = "SELECT * FROM socket_event_log WHERE status='FAILED' ORDER BY received_at DESC LIMIT 100";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private SocketEventLog mapRow(ResultSet rs) throws SQLException {
        SocketEventLog log = new SocketEventLog();
        log.setEventId(rs.getInt("event_id"));
        log.setBranchId(rs.getInt("branch_id"));
        log.setEventType(rs.getString("event_type"));
        log.setPayload(rs.getString("payload"));
        Timestamp ts = rs.getTimestamp("received_at");
        if (ts != null) log.setReceivedAt(ts.toLocalDateTime());
        log.setStatus(rs.getString("status"));
        return log;
    }
}
