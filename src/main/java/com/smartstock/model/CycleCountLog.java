package com.smartstock.model;

import java.time.LocalDate;

/**
 * Maps to the cycle_count_log table (new schema).
 * discrepancy is a GENERATED ALWAYS column = counted_qty - expected_qty (read-only from DB).
 */
public class CycleCountLog implements PhantomEntity {
    private int countId;
    private int branchId;
    private int productId;
    private LocalDate countDate;
    private Integer expectedQty;
    private Integer countedQty;
    private Integer discrepancy; // generated column — read only
    private Integer countedBy;
    private String notes;

    // Display-only fields
    private String productName;
    private String countedByName;

    public CycleCountLog() {}

    public CycleCountLog(int branchId, int productId, LocalDate countDate,
                         int expectedQty, int countedQty, Integer countedBy) {
        this.branchId = branchId;
        this.productId = productId;
        this.countDate = countDate;
        this.expectedQty = expectedQty;
        this.countedQty = countedQty;
        this.countedBy = countedBy;
    }

    @Override public int getId() { return countId; }
    @Override public void setId(int id) { this.countId = id; }

    public int getCountId() { return countId; }
    public void setCountId(int countId) { this.countId = countId; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public LocalDate getCountDate() { return countDate; }
    public void setCountDate(LocalDate countDate) { this.countDate = countDate; }

    public Integer getExpectedQty() { return expectedQty; }
    public void setExpectedQty(Integer expectedQty) { this.expectedQty = expectedQty; }

    public Integer getCountedQty() { return countedQty; }
    public void setCountedQty(Integer countedQty) { this.countedQty = countedQty; }

    /** Read-only — computed by the DB as (counted_qty - expected_qty). */
    public Integer getDiscrepancy() { return discrepancy; }
    public void setDiscrepancy(Integer discrepancy) { this.discrepancy = discrepancy; }

    public Integer getCountedBy() { return countedBy; }
    public void setCountedBy(Integer countedBy) { this.countedBy = countedBy; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getCountedByName() { return countedByName; }
    public void setCountedByName(String countedByName) { this.countedByName = countedByName; }

    /** Convenience: compute discrepancy locally when not yet persisted. */
    public int computeDiscrepancy() {
        return (countedQty != null ? countedQty : 0) - (expectedQty != null ? expectedQty : 0);
    }
}
