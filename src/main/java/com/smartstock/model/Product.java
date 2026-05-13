package com.smartstock.model;

import java.time.LocalDate;

public class Product implements PhantomEntity {
    private int id;
    private String name;
    private String barcode;
    private String qrCode;
    private String qrCodePath;    // Path to QR code image file
    private String category;       // legacy text category (kept for backward compat)
    private Integer categoryId;    // FK → categories.category_id (new)
    private String unit;           // e.g. "KG", "PCS", "BOX" (new)
    private double unitCost;
    private double wholesalePrice; // (new)
    private double sellingPrice;
    private int reorderLevel;      // replaces min_stock concept at product level (new)
    private String description;    // (new)
    private LocalDate expiryDate;
    private Integer branchId;
    private int quantity;
    private int minStock;

    public Product() {}

    public Product(String name, String barcode, String category, double unitCost,
                   double sellingPrice, Integer branchId) {
        this.name = name;
        this.barcode = barcode;
        this.category = category;
        this.unitCost = unitCost;
        this.sellingPrice = sellingPrice;
        this.branchId = branchId;
        this.reorderLevel = 10;
    }

    @Override
    public int getId() { return id; }
    @Override
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }

    public String getQrCodePath() { return qrCodePath; }
    public void setQrCodePath(String qrCodePath) { this.qrCodePath = qrCodePath; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public double getUnitCost() { return unitCost; }
    public void setUnitCost(double unitCost) { this.unitCost = unitCost; }

    public double getWholesalePrice() { return wholesalePrice; }
    public void setWholesalePrice(double wholesalePrice) { this.wholesalePrice = wholesalePrice; }

    public double getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(double sellingPrice) { this.sellingPrice = sellingPrice; }

    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getMinStock() { return minStock; }
    public void setMinStock(int minStock) { this.minStock = minStock; }
}
