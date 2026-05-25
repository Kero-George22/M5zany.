package com.smartstock.model;

/**
 * Maps to the transaction_items table (M4 — POS line items).
 */
public class TransactionItem implements PhantomEntity {
    private int itemId;
    private int transactionId;
    private int productId;
    private int quantity;
    private double unitPrice;
    private double subtotal;

    // Display-only
    private String productName;

    public TransactionItem() {}

    public TransactionItem(int transactionId, int productId, int quantity, double unitPrice) {
        this.transactionId = transactionId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = unitPrice * quantity;
    }

    @Override public int getId() { return itemId; }
    @Override public void setId(int id) { this.itemId = id; }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public int getTransactionId() { return transactionId; }
    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
}
