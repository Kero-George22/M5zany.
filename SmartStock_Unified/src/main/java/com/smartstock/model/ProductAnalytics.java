package com.smartstock.model;

import java.time.LocalDate;

/**
 * Maps to the product_analytics table (M3).
 * classification: FAST_MOVER | SLOW_MOVER | LOSS_MAKER | NORMAL
 */
public class ProductAnalytics implements PhantomEntity {
    private int analyticsId;
    private int branchId;
    private int productId;
    private LocalDate month;
    private int totalSold;
    private double totalRevenue;
    private double totalCost;
    private double profit;
    private String classification; // FAST_MOVER, SLOW_MOVER, LOSS_MAKER, NORMAL

    // Display-only
    private String productName;
    private String branchName;

    public ProductAnalytics() {}

    @Override public int getId() { return analyticsId; }
    @Override public void setId(int id) { this.analyticsId = id; }

    public int getAnalyticsId() { return analyticsId; }
    public void setAnalyticsId(int analyticsId) { this.analyticsId = analyticsId; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public LocalDate getMonth() { return month; }
    public void setMonth(LocalDate month) { this.month = month; }

    public int getTotalSold() { return totalSold; }
    public void setTotalSold(int totalSold) { this.totalSold = totalSold; }

    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }

    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }

    public double getProfit() { return profit; }
    public void setProfit(double profit) { this.profit = profit; }

    public String getClassification() { return classification; }
    public void setClassification(String classification) { this.classification = classification; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public boolean isFastMover() { return "FAST_MOVER".equals(classification); }
    public boolean isLossMaker() { return "LOSS_MAKER".equals(classification); }
}
