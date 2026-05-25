package com.smartstock.dao;

import com.smartstock.model.PricingHistory;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PricingHistoryDAO {

    public int insert(PricingHistory ph) {
        String sql = "INSERT INTO pricing_history (product_id, branch_id, wholesale_price, quantity, " +
                "suggested_price, applied_price, ai_reasoning, suggested_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, ph.getProductId());
            if (ph.getBranchId() != null) stmt.setInt(2, ph.getBranchId());
            else stmt.setNull(2, Types.INTEGER);
            if (ph.getWholesalePrice() != null) stmt.setDouble(3, ph.getWholesalePrice());
            else stmt.setNull(3, Types.DECIMAL);
            if (ph.getQuantity() != null) stmt.setInt(4, ph.getQuantity());
            else stmt.setNull(4, Types.INTEGER);
            if (ph.getSuggestedPrice() != null) stmt.setDouble(5, ph.getSuggestedPrice());
            else stmt.setNull(5, Types.DECIMAL);
            if (ph.getAppliedPrice() != null) stmt.setDouble(6, ph.getAppliedPrice());
            else stmt.setNull(6, Types.DECIMAL);
            stmt.setString(7, ph.getAiReasoning());
            if (ph.getSuggestedBy() != null) stmt.setInt(8, ph.getSuggestedBy());
            else stmt.setNull(8, Types.INTEGER);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public List<PricingHistory> findByProduct(int productId) {
        List<PricingHistory> list = new ArrayList<>();
        String sql = "SELECT ph.*, p.name as product_name, u.full_name as suggested_by_name " +
                "FROM pricing_history ph " +
                "JOIN products p ON ph.product_id = p.id " +
                "LEFT JOIN users u ON ph.suggested_by = u.id " +
                "WHERE ph.product_id = ? ORDER BY ph.suggested_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<PricingHistory> findLatestPerProduct(int branchId) {
        List<PricingHistory> list = new ArrayList<>();
        String sql = "SELECT ph.*, p.name as product_name, u.full_name as suggested_by_name " +
                "FROM pricing_history ph " +
                "JOIN products p ON ph.product_id = p.id " +
                "LEFT JOIN users u ON ph.suggested_by = u.id " +
                "WHERE ph.branch_id = ? " +
                "AND ph.pricing_id = (SELECT MAX(ph2.pricing_id) FROM pricing_history ph2 WHERE ph2.product_id = ph.product_id AND ph2.branch_id = ph.branch_id) " +
                "ORDER BY ph.suggested_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private PricingHistory mapRow(ResultSet rs) throws SQLException {
        PricingHistory ph = new PricingHistory();
        ph.setPricingId(rs.getInt("pricing_id"));
        ph.setProductId(rs.getInt("product_id"));
        ph.setBranchId(rs.getObject("branch_id", Integer.class));
        double wp = rs.getDouble("wholesale_price"); if (!rs.wasNull()) ph.setWholesalePrice(wp);
        int qty = rs.getInt("quantity"); if (!rs.wasNull()) ph.setQuantity(qty);
        double sp = rs.getDouble("suggested_price"); if (!rs.wasNull()) ph.setSuggestedPrice(sp);
        double ap = rs.getDouble("applied_price"); if (!rs.wasNull()) ph.setAppliedPrice(ap);
        ph.setAiReasoning(rs.getString("ai_reasoning"));
        Timestamp ts = rs.getTimestamp("suggested_at");
        if (ts != null) ph.setSuggestedAt(ts.toLocalDateTime());
        ph.setSuggestedBy(rs.getObject("suggested_by", Integer.class));
        try { ph.setProductName(rs.getString("product_name")); } catch (SQLException ignored) {}
        try { ph.setSuggestedByName(rs.getString("suggested_by_name")); } catch (SQLException ignored) {}
        return ph;
    }
}
