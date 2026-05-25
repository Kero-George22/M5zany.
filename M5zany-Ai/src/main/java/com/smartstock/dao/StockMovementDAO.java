package com.smartstock.dao;

import com.smartstock.model.StockMovement;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StockMovementDAO {
    public int insert(StockMovement movement) {
        String sql = "INSERT INTO stock_movements (product_id, branch_id, movement_type, quantity, unit_price, cashier_id, notes) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, movement.getProductId());
            stmt.setInt(2, movement.getBranchId());
            stmt.setString(3, movement.getMovementType());
            stmt.setInt(4, movement.getQuantity());
            stmt.setDouble(5, movement.getUnitPrice());
            if (movement.getCashierId() != null) {
                stmt.setInt(6, movement.getCashierId());
            } else {
                stmt.setNull(6, Types.INTEGER);
            }
            stmt.setString(7, movement.getNotes());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public List<StockMovement> findByBranchId(int branchId) {
        List<StockMovement> movements = new ArrayList<>();
        String sql = "SELECT * FROM stock_movements WHERE branch_id=? ORDER BY created_at DESC LIMIT 100";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    movements.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return movements;
    }

    public List<StockMovement> findAll() {
        List<StockMovement> movements = new ArrayList<>();
        String sql = "SELECT * FROM stock_movements ORDER BY created_at DESC LIMIT 500";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                movements.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return movements;
    }

    public int getSaleCountLast30Days(int productId) {
        String sql = "SELECT COUNT(*) FROM stock_movements WHERE product_id=? AND movement_type='SALE' AND created_at > DATE_SUB(NOW(), INTERVAL 30 DAY)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<StockMovement> findTransfersByBranch(int branchId) {
        List<StockMovement> list = new ArrayList<>();
        String sql = "SELECT sm.*, p.name as product_name, b.name as branch_name " +
                "FROM stock_movements sm " +
                "JOIN products p ON sm.product_id = p.id " +
                "JOIN branches b ON sm.branch_id = b.id " +
                "WHERE sm.branch_id = ? AND sm.movement_type = 'TRANSFER' " +
                "ORDER BY sm.created_at DESC LIMIT 50";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    StockMovement m = mapRow(rs);
                    m.setProductName(rs.getString("product_name"));
                    m.setBranchName(rs.getString("branch_name"));
                    list.add(m);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<StockMovement> findAllTransfers() {
        List<StockMovement> list = new ArrayList<>();
        String sql = "SELECT sm.*, p.name as product_name, b.name as branch_name " +
                "FROM stock_movements sm " +
                "JOIN products p ON sm.product_id = p.id " +
                "JOIN branches b ON sm.branch_id = b.id " +
                "WHERE sm.movement_type = 'TRANSFER' " +
                "ORDER BY sm.created_at DESC LIMIT 100";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                StockMovement m = mapRow(rs);
                m.setProductName(rs.getString("product_name"));
                m.setBranchName(rs.getString("branch_name"));
                list.add(m);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<StockMovement> findRecentByBranch(int branchId) {
        List<StockMovement> list = new ArrayList<>();
        String sql = "SELECT sm.*, p.name as product_name, b.name as branch_name " +
                "FROM stock_movements sm " +
                "JOIN products p ON sm.product_id = p.id " +
                "JOIN branches b ON sm.branch_id = b.id " +
                "WHERE sm.branch_id = ? ORDER BY sm.created_at DESC LIMIT 30";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    StockMovement m = mapRow(rs);
                    m.setProductName(rs.getString("product_name"));
                    m.setBranchName(rs.getString("branch_name"));
                    list.add(m);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private StockMovement mapRow(ResultSet rs) throws SQLException {
        StockMovement m = new StockMovement();
        m.setId(rs.getInt("id"));
        m.setProductId(rs.getInt("product_id"));
        m.setBranchId(rs.getInt("branch_id"));
        m.setMovementType(rs.getString("movement_type"));
        m.setQuantity(rs.getInt("quantity"));
        m.setUnitPrice(rs.getDouble("unit_price"));
        m.setCashierId(rs.getObject("cashier_id", Integer.class));
        m.setNotes(rs.getString("notes"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) m.setCreatedAt(ts.toLocalDateTime());
        return m;
    }
}
