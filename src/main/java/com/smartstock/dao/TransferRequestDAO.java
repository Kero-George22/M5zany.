package com.smartstock.dao;

import com.smartstock.model.TransferRequest;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransferRequestDAO {
    public TransferRequestDAO() {
        ensureTable();
    }

    private void ensureTable() {
        String sql = "CREATE TABLE IF NOT EXISTS transfer_requests (" +
                "request_id INT PRIMARY KEY AUTO_INCREMENT," +
                "product_id INT NOT NULL," +
                "from_branch_id INT NOT NULL," +
                "to_branch_id INT NOT NULL," +
                "quantity INT NOT NULL," +
                "notes TEXT," +
                "requested_by INT NOT NULL," +
                "status VARCHAR(20) NOT NULL DEFAULT 'PENDING'," +
                "approved_by INT NULL," +
                "requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "approved_at TIMESTAMP NULL," +
                "FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE," +
                "FOREIGN KEY (from_branch_id) REFERENCES branches(id) ON DELETE CASCADE," +
                "FOREIGN KEY (to_branch_id) REFERENCES branches(id) ON DELETE CASCADE," +
                "FOREIGN KEY (requested_by) REFERENCES users(id) ON DELETE CASCADE," +
                "FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL" +
                ")";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public int insert(TransferRequest req) {
        String sql = "INSERT INTO transfer_requests (product_id, from_branch_id, to_branch_id, quantity, notes, requested_by, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'PENDING')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, req.getProductId());
            stmt.setInt(2, req.getFromBranchId());
            stmt.setInt(3, req.getToBranchId());
            stmt.setInt(4, req.getQuantity());
            stmt.setString(5, req.getNotes());
            stmt.setInt(6, req.getRequestedBy());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public List<TransferRequest> findPendingForAdmin() {
        return findBySql("WHERE tr.status='PENDING'");
    }

    public List<TransferRequest> findForBranch(int branchId) {
        return findBySql("WHERE tr.from_branch_id = " + branchId + " OR tr.to_branch_id = " + branchId);
    }

    public boolean approve(int requestId, int approverId) {
        String sql = "UPDATE transfer_requests SET status='APPROVED', approved_by=?, approved_at=NOW() WHERE request_id=? AND status='PENDING'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, approverId);
            stmt.setInt(2, requestId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean reject(int requestId, int approverId) {
        String sql = "UPDATE transfer_requests SET status='REJECTED', approved_by=?, approved_at=NOW() WHERE request_id=? AND status='PENDING'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, approverId);
            stmt.setInt(2, requestId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    private List<TransferRequest> findBySql(String whereClause) {
        List<TransferRequest> list = new ArrayList<>();
        String sql = "SELECT tr.*, p.name product_name, fb.name from_branch_name, tb.name to_branch_name, u.full_name requested_by_name " +
                "FROM transfer_requests tr " +
                "JOIN products p ON tr.product_id = p.id " +
                "JOIN branches fb ON tr.from_branch_id = fb.id " +
                "JOIN branches tb ON tr.to_branch_id = tb.id " +
                "JOIN users u ON tr.requested_by = u.id " +
                whereClause + " ORDER BY tr.requested_at DESC LIMIT 200";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private TransferRequest mapRow(ResultSet rs) throws SQLException {
        TransferRequest tr = new TransferRequest();
        tr.setRequestId(rs.getInt("request_id"));
        tr.setProductId(rs.getInt("product_id"));
        tr.setFromBranchId(rs.getInt("from_branch_id"));
        tr.setToBranchId(rs.getInt("to_branch_id"));
        tr.setQuantity(rs.getInt("quantity"));
        tr.setNotes(rs.getString("notes"));
        tr.setRequestedBy(rs.getInt("requested_by"));
        tr.setStatus(rs.getString("status"));
        tr.setApprovedBy(rs.getObject("approved_by", Integer.class));
        Timestamp reqTs = rs.getTimestamp("requested_at");
        if (reqTs != null) tr.setRequestedAt(reqTs.toLocalDateTime());
        Timestamp appTs = rs.getTimestamp("approved_at");
        if (appTs != null) tr.setApprovedAt(appTs.toLocalDateTime());
        tr.setProductName(rs.getString("product_name"));
        tr.setFromBranchName(rs.getString("from_branch_name"));
        tr.setToBranchName(rs.getString("to_branch_name"));
        tr.setRequestedByName(rs.getString("requested_by_name"));
        return tr;
    }
}
