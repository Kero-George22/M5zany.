package com.smartstock.model;

import java.time.LocalDate;

public class CycleCountRecord implements PhantomEntity {
    private int id;
    private int productId;
    private int branchId;
    private int countedBy;
    private int expectedQty;
    private int actualQty;
    private int variance;
    private LocalDate countDate;
    private String notes;

    @Override public int getId() { return id; }
    @Override public void setId(int id) { this.id = id; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }

    public int getCountedBy() { return countedBy; }
    public void setCountedBy(int countedBy) { this.countedBy = countedBy; }

    public int getExpectedQty() { return expectedQty; }
    public void setExpectedQty(int expectedQty) { this.expectedQty = expectedQty; }

    public int getActualQty() { return actualQty; }
    public void setActualQty(int actualQty) { this.actualQty = actualQty; }

    public int getVariance() { return variance; }
    public void setVariance(int variance) { this.variance = variance; }

    public LocalDate getCountDate() { return countDate; }
    public void setCountDate(LocalDate countDate) { this.countDate = countDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
