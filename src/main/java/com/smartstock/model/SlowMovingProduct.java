package com.smartstock.model;

/**
 * Represents a product with low sales activity (Module 2 - Product Analysis).
 */
public class SlowMovingProduct implements PhantomEntity {
    private int id;
    private String productName;
    private int branchId;
    private String branchName;
    private int quantity;
    private int salesCount;
    private int minStock;

    public SlowMovingProduct() {}

    @Override
    public int getId() { return id; }
    @Override
    public void setId(int id) { this.id = id; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getSalesCount() { return salesCount; }
    public void setSalesCount(int salesCount) { this.salesCount = salesCount; }

    public int getMinStock() { return minStock; }
    public void setMinStock(int minStock) { this.minStock = minStock; }
}
