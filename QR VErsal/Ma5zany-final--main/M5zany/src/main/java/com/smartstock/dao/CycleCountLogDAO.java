package com.smartstock.dao;

import com.smartstock.model.CycleCountLog;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the cycle_count_log table (new schema).
 * Note: discrepancy is a GENERATED column — never included in INSERT.
 */
public class CycleCountLogDAO {

    public int insert(CycleCountLog log) {
        String sql = "INSERT INTO cycle_count_log (branch_id, product_id, count_date, expected_qty, counted_qty, counted_by, notes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, log.getBranchId());
            stmt.setInt(2, log.getProductId());
            stmt.setDate(3, Date.valueOf(log.getCountDate()));
            if (log.getExpectedQty() != null) stmt.setInt(4, log.getExpectedQty());
            else stmt.setNull(4, Types.INTEGER);
            if (log.getCountedQty() != null) stmt.setInt(5, log.getCountedQty());
            else stmt.setNull(5, Types.INTEGER);
            if (log.getCountedBy() != null) stmt.setInt(6, log.getCountedBy());
            else stmt.setNull(6, Types.INTEGER);
            stmt.setString(7, log.getNotes());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public List<CycleCountLog> findByBranch(int branchId) {
        List<CycleCountLog> list = new ArrayList<>();
        String sql = "SELECT ccl.*, p.name as product_name, u.full_name as counted_by_name " +
                "FROM cycle_count_log ccl " +
                "JOIN products p ON ccl.product_id = p.id " +
                "LEFT JOIN users u ON ccl.counted_by = u.id " +
                "WHERE ccl.branch_id = ? ORDER BY ccl.count_date DESC LIMIT 200";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<CycleCountLog> findDiscrepancies(int branchId) {
        List<CycleCountLog> list = new ArrayList<>();
        String sql = "SELECT ccl.*, p.name as product_name, u.full_name as counted_by_name " +
                "FROM cycle_count_log ccl " +
                "JOIN products p ON ccl.product_id = p.id " +
                "LEFT JOIN users u ON ccl.counted_by = u.id " +
                "WHERE ccl.branch_id = ? AND ccl.discrepancy <> 0 ORDER BY ABS(ccl.discrepancy) DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private CycleCountLog mapRow(ResultSet rs) throws SQLException {
        CycleCountLog c = new CycleCountLog();
        c.setCountId(rs.getInt("count_id"));
        c.setBranchId(rs.getInt("branch_id"));
        c.setProductId(rs.getInt("product_id"));
        Date d = rs.getDate("count_date");
        if (d != null) c.setCountDate(d.toLocalDate());
        int eq = rs.getInt("expected_qty"); if (!rs.wasNull()) c.setExpectedQty(eq);
        int cq = rs.getInt("counted_qty"); if (!rs.wasNull()) c.setCountedQty(cq);
        int disc = rs.getInt("discrepancy"); if (!rs.wasNull()) c.setDiscrepancy(disc);
        int cb = rs.getInt("counted_by"); if (!rs.wasNull()) c.setCountedBy(cb);
        c.setNotes(rs.getString("notes"));
        try { c.setProductName(rs.getString("product_name")); } catch (SQLException ignored) {}
        try { c.setCountedByName(rs.getString("counted_by_name")); } catch (SQLException ignored) {}
        return c;
    }
}
