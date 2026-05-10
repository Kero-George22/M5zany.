package com.smartstock.dao;

import com.smartstock.model.ProductAnalytics;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProductAnalyticsDAO {

    public int insert(ProductAnalytics pa) {
        String sql = "INSERT INTO product_analytics (branch_id, product_id, month, total_sold, total_revenue, total_cost, profit, classification) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, pa.getBranchId());
            stmt.setInt(2, pa.getProductId());
            stmt.setDate(3, Date.valueOf(pa.getMonth()));
            stmt.setInt(4, pa.getTotalSold());
            stmt.setDouble(5, pa.getTotalRevenue());
            stmt.setDouble(6, pa.getTotalCost());
            stmt.setDouble(7, pa.getProfit());
            stmt.setString(8, pa.getClassification() != null ? pa.getClassification() : "NORMAL");
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public List<ProductAnalytics> findByBranchAndMonth(int branchId, LocalDate month) {
        List<ProductAnalytics> list = new ArrayList<>();
        String sql = "SELECT pa.*, p.name as product_name, b.name as branch_name " +
                "FROM product_analytics pa " +
                "JOIN products p ON pa.product_id = p.id " +
                "JOIN branches b ON pa.branch_id = b.id " +
                "WHERE pa.branch_id = ? AND pa.month = ? ORDER BY pa.total_sold DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            stmt.setDate(2, Date.valueOf(month));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<ProductAnalytics> findByClassification(int branchId, String classification) {
        List<ProductAnalytics> list = new ArrayList<>();
        String sql = "SELECT pa.*, p.name as product_name, b.name as branch_name " +
                "FROM product_analytics pa " +
                "JOIN products p ON pa.product_id = p.id " +
                "JOIN branches b ON pa.branch_id = b.id " +
                "WHERE pa.branch_id = ? AND pa.classification = ? ORDER BY pa.month DESC, pa.total_sold DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            stmt.setString(2, classification);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private ProductAnalytics mapRow(ResultSet rs) throws SQLException {
        ProductAnalytics pa = new ProductAnalytics();
        pa.setAnalyticsId(rs.getInt("analytics_id"));
        pa.setBranchId(rs.getInt("branch_id"));
        pa.setProductId(rs.getInt("product_id"));
        Date d = rs.getDate("month");
        if (d != null) pa.setMonth(d.toLocalDate());
        pa.setTotalSold(rs.getInt("total_sold"));
        pa.setTotalRevenue(rs.getDouble("total_revenue"));
        pa.setTotalCost(rs.getDouble("total_cost"));
        pa.setProfit(rs.getDouble("profit"));
        pa.setClassification(rs.getString("classification"));
        try { pa.setProductName(rs.getString("product_name")); } catch (SQLException ignored) {}
        try { pa.setBranchName(rs.getString("branch_name")); } catch (SQLException ignored) {}
        return pa;
    }
}
