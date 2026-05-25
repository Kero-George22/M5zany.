package com.smartstock.model;

/**
 * Represents a product where unit cost exceeds selling price (Module 2 - Product Analysis).
 */
public class LossMakingProduct implements PhantomEntity {
    private int id;
    private String productName;
    private int branchId;
    private String branchName;
    private double unitCost;
    private double sellingPrice;
    private double lossPerUnit;
    private int quantity;
    private double totalLoss;

    public LossMakingProduct() {}

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

    public double getUnitCost() { return unitCost; }
    public void setUnitCost(double unitCost) { this.unitCost = unitCost; }

    public double getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(double sellingPrice) { this.sellingPrice = sellingPrice; }

    public double getLossPerUnit() { return lossPerUnit; }
    public void setLossPerUnit(double lossPerUnit) { this.lossPerUnit = lossPerUnit; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getTotalLoss() { return totalLoss; }
    public void setTotalLoss(double totalLoss) { this.totalLoss = totalLoss; }
}
