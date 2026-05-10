package com.smartstock.dao;

import com.smartstock.model.FinancialSummary;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FinancialSummaryDAO {

    /**
     * Upserts a daily financial summary (INSERT ... ON DUPLICATE KEY UPDATE).
     * Unique key: (branch_id, summary_date).
     */
    public boolean upsert(FinancialSummary fs) {
        String sql = "INSERT INTO financial_summary (branch_id, summary_date, total_revenue, total_cost, total_profit, transaction_count) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE total_revenue=VALUES(total_revenue), total_cost=VALUES(total_cost), " +
                "total_profit=VALUES(total_profit), transaction_count=VALUES(transaction_count)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, fs.getBranchId());
            stmt.setDate(2, Date.valueOf(fs.getSummaryDate()));
            stmt.setDouble(3, fs.getTotalRevenue());
            stmt.setDouble(4, fs.getTotalCost());
            stmt.setDouble(5, fs.getTotalProfit());
            stmt.setInt(6, fs.getTransactionCount());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public FinancialSummary findByBranchAndDate(int branchId, LocalDate date) {
        String sql = "SELECT fs.*, b.name as branch_name FROM financial_summary fs " +
                "JOIN branches b ON fs.branch_id = b.id WHERE fs.branch_id=? AND fs.summary_date=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            stmt.setDate(2, Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public List<FinancialSummary> findByBranch(int branchId, int limit) {
        List<FinancialSummary> list = new ArrayList<>();
        String sql = "SELECT fs.*, b.name as branch_name FROM financial_summary fs " +
                "JOIN branches b ON fs.branch_id = b.id " +
                "WHERE fs.branch_id=? ORDER BY fs.summary_date DESC LIMIT ?";
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

    /**
     * Recalculates and upserts today's summary for a branch from actual transactions.
     */
    public boolean recalculateToday(int branchId) {
        String sql = "INSERT INTO financial_summary (branch_id, summary_date, total_revenue, total_cost, total_profit, transaction_count) " +
                "SELECT t.branch_id, CURDATE(), " +
                "  SUM(t.final_amount), " +
                "  COALESCE((SELECT SUM(ti.quantity * p.unit_cost) FROM transaction_items ti JOIN products p ON ti.product_id = p.id JOIN transactions t2 ON ti.transaction_id = t2.transaction_id WHERE t2.branch_id = ? AND DATE(t2.transaction_at) = CURDATE() AND t2.status = 'COMPLETED'), 0), " +
                "  SUM(t.final_amount) - COALESCE((SELECT SUM(ti.quantity * p.unit_cost) FROM transaction_items ti JOIN products p ON ti.product_id = p.id JOIN transactions t2 ON ti.transaction_id = t2.transaction_id WHERE t2.branch_id = ? AND DATE(t2.transaction_at) = CURDATE() AND t2.status = 'COMPLETED'), 0), " +
                "  COUNT(*) " +
                "FROM transactions t WHERE t.branch_id = ? AND DATE(t.transaction_at) = CURDATE() AND t.status = 'COMPLETED' " +
                "ON DUPLICATE KEY UPDATE " +
                "  total_revenue = VALUES(total_revenue), total_cost = VALUES(total_cost), " +
                "  total_profit = VALUES(total_profit), transaction_count = VALUES(transaction_count)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            stmt.setInt(2, branchId);
            stmt.setInt(3, branchId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    private FinancialSummary mapRow(ResultSet rs) throws SQLException {
        FinancialSummary fs = new FinancialSummary();
        fs.setSummaryId(rs.getInt("summary_id"));
        fs.setBranchId(rs.getInt("branch_id"));
        Date d = rs.getDate("summary_date");
        if (d != null) fs.setSummaryDate(d.toLocalDate());
        fs.setTotalRevenue(rs.getDouble("total_revenue"));
        fs.setTotalCost(rs.getDouble("total_cost"));
        fs.setTotalProfit(rs.getDouble("total_profit"));
        fs.setTransactionCount(rs.getInt("transaction_count"));
        try { fs.setBranchName(rs.getString("branch_name")); } catch (SQLException ignored) {}
        return fs;
    }
}
