package com.smartstock.dao;

import com.smartstock.model.Transaction;
import com.smartstock.model.TransactionItem;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {

    /**
     * Inserts a transaction along with all its items in a single DB transaction.
     * Returns the generated transaction_id, or -1 on failure.
     */
    public int insertWithItems(Transaction txn) {
        String txnSql = "INSERT INTO transactions (branch_id, cashier_id, total_amount, discount_amount, final_amount, payment_method, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        String itemSql = "INSERT INTO transaction_items (transaction_id, product_id, quantity, unit_price, subtotal) VALUES (?, ?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            int txnId;
            try (PreparedStatement stmt = conn.prepareStatement(txnSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, txn.getBranchId());
                stmt.setInt(2, txn.getCashierId());
                stmt.setDouble(3, txn.getTotalAmount());
                stmt.setDouble(4, txn.getDiscountAmount());
                stmt.setDouble(5, txn.getFinalAmount());
                stmt.setString(6, txn.getPaymentMethod() != null ? txn.getPaymentMethod() : "CASH");
                stmt.setString(7, txn.getStatus() != null ? txn.getStatus() : "COMPLETED");
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (!rs.next()) { conn.rollback(); return -1; }
                    txnId = rs.getInt(1);
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement(itemSql)) {
                for (TransactionItem item : txn.getItems()) {
                    stmt.setInt(1, txnId);
                    stmt.setInt(2, item.getProductId());
                    stmt.setInt(3, item.getQuantity());
                    stmt.setDouble(4, item.getUnitPrice());
                    stmt.setDouble(5, item.getSubtotal());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
            conn.commit();
            return txnId;
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); } }
        } finally {
            if (conn != null) { try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { ex.printStackTrace(); } }
        }
        return -1;
    }

    public boolean updateStatus(int transactionId, String status) {
        String sql = "UPDATE transactions SET status=? WHERE transaction_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, transactionId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public List<Transaction> findByBranch(int branchId) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT t.*, u.full_name as cashier_name, b.name as branch_name " +
                "FROM transactions t " +
                "JOIN users u ON t.cashier_id = u.id " +
                "JOIN branches b ON t.branch_id = b.id " +
                "WHERE t.branch_id = ? ORDER BY t.transaction_at DESC LIMIT 200";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapTxnRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Transaction> findByCashier(int cashierId) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT t.*, u.full_name as cashier_name, b.name as branch_name " +
                "FROM transactions t " +
                "JOIN users u ON t.cashier_id = u.id " +
                "JOIN branches b ON t.branch_id = b.id " +
                "WHERE t.cashier_id = ? ORDER BY t.transaction_at DESC LIMIT 100";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, cashierId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapTxnRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<TransactionItem> findItemsByTransaction(int transactionId) {
        List<TransactionItem> list = new ArrayList<>();
        String sql = "SELECT ti.*, p.name as product_name FROM transaction_items ti " +
                "JOIN products p ON ti.product_id = p.id WHERE ti.transaction_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, transactionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TransactionItem item = new TransactionItem();
                    item.setItemId(rs.getInt("item_id"));
                    item.setTransactionId(rs.getInt("transaction_id"));
                    item.setProductId(rs.getInt("product_id"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setUnitPrice(rs.getDouble("unit_price"));
                    item.setSubtotal(rs.getDouble("subtotal"));
                    try { item.setProductName(rs.getString("product_name")); } catch (SQLException ignored) {}
                    list.add(item);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private Transaction mapTxnRow(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setTransactionId(rs.getInt("transaction_id"));
        t.setBranchId(rs.getInt("branch_id"));
        t.setCashierId(rs.getInt("cashier_id"));
        t.setTotalAmount(rs.getDouble("total_amount"));
        t.setDiscountAmount(rs.getDouble("discount_amount"));
        t.setFinalAmount(rs.getDouble("final_amount"));
        t.setPaymentMethod(rs.getString("payment_method"));
        t.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("transaction_at");
        if (ts != null) t.setTransactionAt(ts.toLocalDateTime());
        try { t.setCashierName(rs.getString("cashier_name")); } catch (SQLException ignored) {}
        try { t.setBranchName(rs.getString("branch_name")); } catch (SQLException ignored) {}
        return t;
    }
}
