package com.smartstock.model;

import java.time.LocalDate;

/**
 * Maps to the financial_summary table (M4).
 * Unique per (branch_id, summary_date).
 */
public class FinancialSummary implements PhantomEntity {
    private int summaryId;
    private int branchId;
    private LocalDate summaryDate;
    private double totalRevenue;
    private double totalCost;
    private double totalProfit;
    private int transactionCount;
    private String branchName; // display-only

    public FinancialSummary() {}

    @Override public int getId() { return summaryId; }
    @Override public void setId(int id) { this.summaryId = id; }

    public int getSummaryId() { return summaryId; }
    public void setSummaryId(int summaryId) { this.summaryId = summaryId; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }

    public LocalDate getSummaryDate() { return summaryDate; }
    public void setSummaryDate(LocalDate summaryDate) { this.summaryDate = summaryDate; }

    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }

    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }

    public double getTotalProfit() { return totalProfit; }
    public void setTotalProfit(double totalProfit) { this.totalProfit = totalProfit; }

    public int getTransactionCount() { return transactionCount; }
    public void setTransactionCount(int transactionCount) { this.transactionCount = transactionCount; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public double getProfitMargin() {
        return totalRevenue > 0 ? (totalProfit / totalRevenue) * 100 : 0;
    }
}
