package com.smartstock.model;

import java.time.LocalDateTime;

public class TransferRequest implements PhantomEntity {
    private int requestId;
    private int productId;
    private int fromBranchId;
    private int toBranchId;
    private int quantity;
    private String notes;
    private int requestedBy;
    private String status;
    private Integer approvedBy;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;

    private String productName;
    private String fromBranchName;
    private String toBranchName;
    private String requestedByName;

    @Override public int getId() { return requestId; }
    @Override public void setId(int id) { this.requestId = id; }

    public int getRequestId() { return requestId; }
    public void setRequestId(int requestId) { this.requestId = requestId; }
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    public int getFromBranchId() { return fromBranchId; }
    public void setFromBranchId(int fromBranchId) { this.fromBranchId = fromBranchId; }
    public int getToBranchId() { return toBranchId; }
    public void setToBranchId(int toBranchId) { this.toBranchId = toBranchId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public int getRequestedBy() { return requestedBy; }
    public void setRequestedBy(int requestedBy) { this.requestedBy = requestedBy; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Integer approvedBy) { this.approvedBy = approvedBy; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getFromBranchName() { return fromBranchName; }
    public void setFromBranchName(String fromBranchName) { this.fromBranchName = fromBranchName; }
    public String getToBranchName() { return toBranchName; }
    public void setToBranchName(String toBranchName) { this.toBranchName = toBranchName; }
    public String getRequestedByName() { return requestedByName; }
    public void setRequestedByName(String requestedByName) { this.requestedByName = requestedByName; }

    public String getRequestedAtFormatted() {
        if (requestedAt == null) return "";
        return requestedAt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
