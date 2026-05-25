package com.erp.m5any.core;

/**
 * Mandatory Generic Entity Type for the Retail ERP Project.
 * Used as a base for JDBC-based data persistence.
 */
public abstract class PhantomEntity {
    protected int id;
    protected String barcode;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
}
