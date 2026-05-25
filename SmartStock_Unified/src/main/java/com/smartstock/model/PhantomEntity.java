package com.smartstock.model;

/**
 * Mandatory Generic Entity Type for the Retail ERP Project.
 * Used as a base for JDBC-based data persistence.
 */
public interface PhantomEntity {
    int getId();
    void setId(int id);
    
    default String getBarcode() { return null; }
    default void setBarcode(String barcode) {}
}
