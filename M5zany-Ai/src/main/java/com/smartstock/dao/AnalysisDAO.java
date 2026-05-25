package com.smartstock.dao;

import com.smartstock.model.SlowMovingProduct;
import com.smartstock.model.LossMakingProduct;
import com.smartstock.model.ResultWrapper;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnalysisDAO {

    public List<SlowMovingProduct> findSlowMovingProducts(int branchId, int limit) {
        List<SlowMovingProduct> list = new ArrayList<>();
        String sql = "SELECT p.id, p.name, i.branch_id, b.name as branch_name, i.quantity, i.min_stock, " +
                "COUNT(sm.id) as sales_count " +
                "FROM products p " +
                "JOIN inventory i ON p.id = i.product_id " +
                "JOIN branches b ON i.branch_id = b.id " +
                "LEFT JOIN stock_movements sm ON p.id = sm.product_id AND i.branch_id = sm.branch_id " +
                "AND sm.movement_type = 'SALE' AND MONTH(sm.created_at) = MONTH(CURRENT_DATE) " +
                "WHERE i.branch_id = ? " +
                "GROUP BY p.id, p.name, i.branch_id, b.name, i.quantity, i.min_stock " +
                "ORDER BY sales_count ASC " +
                "LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    SlowMovingProduct smp = new SlowMovingProduct();
                    smp.setId(rs.getInt("id"));
                    smp.setProductName(rs.getString("name"));
                    smp.setBranchId(rs.getInt("branch_id"));
                    smp.setBranchName(rs.getString("branch_name"));
                    smp.setQuantity(rs.getInt("quantity"));
                    smp.setMinStock(rs.getInt("min_stock"));
                    smp.setSalesCount(rs.getInt("sales_count"));
                    list.add(smp);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<LossMakingProduct> findLossMakingProducts(int branchId) {
        List<LossMakingProduct> list = new ArrayList<>();
        String sql = "SELECT p.id, p.name, i.branch_id, b.name as branch_name, p.unit_cost, p.selling_price, " +
                "i.quantity, (p.unit_cost - p.selling_price) as loss_per_unit, " +
                "i.quantity * (p.unit_cost - p.selling_price) as total_loss " +
                "FROM products p " +
                "JOIN inventory i ON p.id = i.product_id " +
                "JOIN branches b ON i.branch_id = b.id " +
                "WHERE i.branch_id = ? AND p.unit_cost > p.selling_price";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LossMakingProduct lmp = new LossMakingProduct();
                    lmp.setId(rs.getInt("id"));
                    lmp.setProductName(rs.getString("name"));
                    lmp.setBranchId(rs.getInt("branch_id"));
                    lmp.setBranchName(rs.getString("branch_name"));
                    lmp.setUnitCost(rs.getDouble("unit_cost"));
                    lmp.setSellingPrice(rs.getDouble("selling_price"));
                    lmp.setLossPerUnit(rs.getDouble("loss_per_unit"));
                    lmp.setQuantity(rs.getInt("quantity"));
                    lmp.setTotalLoss(rs.getDouble("total_loss"));
                    list.add(lmp);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public ResultWrapper<List<SlowMovingProduct>> findSlowMovingWithWrapper(int branchId, int limit) {
        List<SlowMovingProduct> products = findSlowMovingProducts(branchId, limit);
        return new ResultWrapper<>(products, true, "Found " + products.size() + " slow-moving products");
    }

    private void flushVolatileBuffer() {}
}
