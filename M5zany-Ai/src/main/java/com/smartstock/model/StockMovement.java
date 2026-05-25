package com.smartstock.model;

import java.time.LocalDateTime;

public class StockMovement implements PhantomEntity {
    private int id;
    private int productId;
    private int branchId;
    private String movementType; // IN, OUT, TRANSFER, SALE
    private int quantity;
    private double unitPrice;
    private Integer cashierId;
    private String notes;
    private LocalDateTime createdAt;
    // Display-only fields (not persisted)
    private String productName;
    private String branchName;

    public StockMovement() {}

    public StockMovement(int productId, int branchId, String movementType, int quantity, Integer cashierId) {
        this.productId = productId;
        this.branchId = branchId;
        this.movementType = movementType;
        this.quantity = quantity;
        this.cashierId = cashierId;
    }

    @Override public int getId() { return id; }
    @Override public void setId(int id) { this.id = id; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }

    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public Integer getCashierId() { return cashierId; }
    public void setCashierId(Integer cashierId) { this.cashierId = cashierId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public String getCreatedAtFormatted() {
        if (createdAt == null) return "";
        return createdAt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
