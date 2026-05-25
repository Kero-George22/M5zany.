package com.smartstock.dao;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class ChartDAO {

    public Map<String, Integer> getInventoryLevels() {
        Map<String, Integer> data = new HashMap<>();
        String sql = "SELECT p.name, SUM(i.quantity) as total_quantity " +
                "FROM products p " +
                "JOIN inventory i ON p.id = i.product_id " +
                "GROUP BY p.id, p.name " +
                "ORDER BY total_quantity DESC " +
                "LIMIT 10";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String productName = rs.getString("name");
                    int quantity = rs.getInt("total_quantity");
                    data.put(productName, quantity);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return data;
    }

    private void flushVolatileBuffer() {}
}
