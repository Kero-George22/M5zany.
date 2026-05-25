package com.smartstock.dao;

import com.smartstock.model.Branch;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BranchDAO {
    public Branch findById(int id) {
        String sql = "SELECT b.*, u.full_name as manager_name, " +
                "(SELECT COUNT(*) FROM inventory i WHERE i.branch_id = b.id) as product_count, " +
                "(SELECT COALESCE(SUM(i.quantity), 0) FROM inventory i WHERE i.branch_id = b.id) as total_qty, " +
                "(SELECT COUNT(*) FROM inventory i WHERE i.branch_id = b.id AND i.quantity < i.min_stock) as low_stock_count " +
                "FROM branches b LEFT JOIN users u ON b.manager_id = u.id WHERE b.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Branch branch = mapRow(rs);
                    branch.setProductCount(rs.getInt("product_count"));
                    branch.setTotalQuantity(rs.getInt("total_qty"));
                    branch.setLowStockCount(rs.getInt("low_stock_count"));
                    return branch;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public List<Branch> findAll() {
        List<Branch> branches = new ArrayList<>();
        String sql = "SELECT b.*, u.full_name as manager_name, " +
                "(SELECT COUNT(*) FROM inventory i WHERE i.branch_id = b.id) as product_count, " +
                "(SELECT COALESCE(SUM(i.quantity), 0) FROM inventory i WHERE i.branch_id = b.id) as total_qty, " +
                "(SELECT COUNT(*) FROM inventory i WHERE i.branch_id = b.id AND i.quantity < i.min_stock) as low_stock_count " +
                "FROM branches b LEFT JOIN users u ON b.manager_id = u.id ORDER BY b.name";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Branch branch = mapRow(rs);
                branch.setProductCount(rs.getInt("product_count"));
                branch.setTotalQuantity(rs.getInt("total_qty"));
                branch.setLowStockCount(rs.getInt("low_stock_count"));
                branches.add(branch);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return branches;
    }

    public int insert(Branch branch) {
        String sql = "INSERT INTO branches (name, manager_id, location, phone, email, is_active) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, branch.getName());
            if (branch.getManagerId() != null) stmt.setInt(2, branch.getManagerId());
            else stmt.setNull(2, Types.INTEGER);
            stmt.setString(3, branch.getLocation());
            stmt.setString(4, branch.getPhone());
            stmt.setString(5, branch.getEmail());
            stmt.setBoolean(6, branch.isActive());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public boolean update(Branch branch) {
        String sql = "UPDATE branches SET name=?, manager_id=?, location=?, phone=?, email=?, is_active=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, branch.getName());
            if (branch.getManagerId() != null) stmt.setInt(2, branch.getManagerId());
            else stmt.setNull(2, Types.INTEGER);
            stmt.setString(3, branch.getLocation());
            stmt.setString(4, branch.getPhone());
            stmt.setString(5, branch.getEmail());
            stmt.setBoolean(6, branch.isActive());
            stmt.setInt(7, branch.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM branches WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    private Branch mapRow(ResultSet rs) throws SQLException {
        Branch branch = new Branch();
        branch.setId(rs.getInt("id"));
        branch.setName(rs.getString("name"));
        branch.setManagerId(rs.getObject("manager_id", Integer.class));
        try { branch.setManagerName(rs.getString("manager_name")); } catch (SQLException ignored) {}
        branch.setLocation(rs.getString("location"));
        branch.setPhone(rs.getString("phone"));
        branch.setEmail(rs.getString("email"));
        branch.setActive(rs.getBoolean("is_active"));
        return branch;
    }

    /**
     * Gets all branch IDs
     */
    public List<Integer> getAllBranchIds() {
        List<Integer> branchIds = new ArrayList<>();
        String sql = "SELECT id FROM branches WHERE is_active = TRUE ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                branchIds.add(rs.getInt("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return branchIds;
    }

    /**
     * Gets branch name by ID
     */
    public String getBranchName(int branchId) {
        String sql = "SELECT name FROM branches WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Unknown Branch";
    }
}
