package com.smartstock.model;

import java.io.Serializable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps to the transactions table (M4 — POS).
 * payment_method: CASH | CARD | WALLET
 * status: COMPLETED | REFUNDED | VOIDED
 */
public class Transaction implements PhantomEntity, Serializable {
    private int transactionId;
    private int branchId;
    private int cashierId;
    private double totalAmount;
    private double discountAmount;
    private double finalAmount;
    private String paymentMethod; // CASH, CARD, WALLET
    private String status;        // COMPLETED, REFUNDED, VOIDED
    private LocalDateTime transactionAt;

    // Display-only
    private String cashierName;
    private String branchName;
    private List<TransactionItem> items = new ArrayList<>();

    public Transaction() {}

    public Transaction(int branchId, int cashierId, double totalAmount,
                       double discountAmount, double finalAmount, String paymentMethod) {
        this.branchId = branchId;
        this.cashierId = cashierId;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.paymentMethod = paymentMethod;
        this.status = "COMPLETED";
    }

    @Override public int getId() { return transactionId; }
    @Override public void setId(int id) { this.transactionId = id; }

    public int getTransactionId() { return transactionId; }
    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }

    public int getCashierId() { return cashierId; }
    public void setCashierId(int cashierId) { this.cashierId = cashierId; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }

    public double getFinalAmount() { return finalAmount; }
    public void setFinalAmount(double finalAmount) { this.finalAmount = finalAmount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getTransactionAt() { return transactionAt; }
    public void setTransactionAt(LocalDateTime transactionAt) { this.transactionAt = transactionAt; }

    public String getCashierName() { return cashierName; }
    public void setCashierName(String cashierName) { this.cashierName = cashierName; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public List<TransactionItem> getItems() { return items; }
    public void setItems(List<TransactionItem> items) { this.items = items; }
    public void addItem(TransactionItem item) { this.items.add(item); }

    public boolean isCompleted() { return "COMPLETED".equals(status); }
    public boolean isRefunded() { return "REFUNDED".equals(status); }

    public String getTransactionAtFormatted() {
        if (transactionAt == null) return "";
        return transactionAt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
