package com.smartstock.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Maps to the expiry_alerts table — more specialized than the general alerts table.
 * status: PENDING | ACKNOWLEDGED | RESOLVED
 */
public class ExpiryAlert implements PhantomEntity {
    private int alertId;
    private int branchId;
    private int productId;
    private LocalDate expiryDate;
    private Integer daysRemaining;
    private String status; // PENDING, ACKNOWLEDGED, RESOLVED
    private LocalDateTime createdAt;

    // Display-only fields
    private String productName;
    private String branchName;

    public ExpiryAlert() {}

    public ExpiryAlert(int branchId, int productId, LocalDate expiryDate, int daysRemaining) {
        this.branchId = branchId;
        this.productId = productId;
        this.expiryDate = expiryDate;
        this.daysRemaining = daysRemaining;
        this.status = "PENDING";
    }

    @Override public int getId() { return alertId; }
    @Override public void setId(int id) { this.alertId = id; }

    public int getAlertId() { return alertId; }
    public void setAlertId(int alertId) { this.alertId = alertId; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public Integer getDaysRemaining() { return daysRemaining; }
    public void setDaysRemaining(Integer daysRemaining) { this.daysRemaining = daysRemaining; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public boolean isPending() { return "PENDING".equals(status); }
    public boolean isResolved() { return "RESOLVED".equals(status); }
}
