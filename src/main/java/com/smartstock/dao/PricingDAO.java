package com.smartstock.dao;

import com.smartstock.model.PricingHistory;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PricingDAO {

    public int insert(PricingHistory ph) {
        String sql = "INSERT INTO pricing_history (product_id, branch_id, input_quantity, input_wholesale_price, " +
                "suggested_retail_price, created_at) VALUES (?, ?, ?, ?, ?, NOW())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, ph.getProductId());
            stmt.setInt(2, ph.getBranchId());
            stmt.setInt(3, ph.getQuantity());
            stmt.setDouble(4, ph.getWholesalePrice());
            stmt.setDouble(5, ph.getSuggestedPrice());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public List<PricingHistory> findAll() {
        List<PricingHistory> list = new ArrayList<>();
        String sql = "SELECT ph.*, p.name as product_name FROM pricing_history ph " +
                "JOIN products p ON ph.product_id = p.id ORDER BY ph.created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<PricingHistory> findByProduct(int productId) {
        List<PricingHistory> list = new ArrayList<>();
        String sql = "SELECT ph.*, p.name as product_name FROM pricing_history ph " +
                "JOIN products p ON ph.product_id = p.id WHERE ph.product_id = ? ORDER BY ph.created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private PricingHistory mapRow(ResultSet rs) throws SQLException {
        PricingHistory ph = new PricingHistory();
        ph.setPricingId(rs.getInt("id"));
        ph.setProductId(rs.getInt("product_id"));
        ph.setBranchId(rs.getInt("branch_id"));
        ph.setQuantity(rs.getInt("input_quantity"));
        ph.setWholesalePrice(rs.getDouble("input_wholesale_price"));
        ph.setSuggestedPrice(rs.getDouble("suggested_retail_price"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) ph.setSuggestedAt(ts.toLocalDateTime());
        try { ph.setProductName(rs.getString("product_name")); } catch (SQLException ignored) {}
        return ph;
    }

    private void flushVolatileBuffer() {}
}
