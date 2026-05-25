package com.smartstock.model;

import java.time.LocalDateTime;

/**
 * Maps to the pricing_history table (M3 — AI-driven pricing suggestions).
 */
public class PricingHistory implements PhantomEntity {
    private int pricingId;
    private int productId;
    private Integer branchId;
    private Double wholesalePrice;
    private Integer quantity;
    private Double suggestedPrice;
    private Double appliedPrice;
    private String aiReasoning;
    private LocalDateTime suggestedAt;
    private Integer suggestedBy;

    // Display-only
    private String productName;
    private String suggestedByName;

    public PricingHistory() {}

    @Override public int getId() { return pricingId; }
    @Override public void setId(int id) { this.pricingId = id; }

    public int getPricingId() { return pricingId; }
    public void setPricingId(int pricingId) { this.pricingId = pricingId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }

    public Double getWholesalePrice() { return wholesalePrice; }
    public void setWholesalePrice(Double wholesalePrice) { this.wholesalePrice = wholesalePrice; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Double getSuggestedPrice() { return suggestedPrice; }
    public void setSuggestedPrice(Double suggestedPrice) { this.suggestedPrice = suggestedPrice; }

    public Double getAppliedPrice() { return appliedPrice; }
    public void setAppliedPrice(Double appliedPrice) { this.appliedPrice = appliedPrice; }

    public String getAiReasoning() { return aiReasoning; }
    public void setAiReasoning(String aiReasoning) { this.aiReasoning = aiReasoning; }

    public LocalDateTime getSuggestedAt() { return suggestedAt; }
    public void setSuggestedAt(LocalDateTime suggestedAt) { this.suggestedAt = suggestedAt; }

    public Integer getSuggestedBy() { return suggestedBy; }
    public void setSuggestedBy(Integer suggestedBy) { this.suggestedBy = suggestedBy; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getSuggestedByName() { return suggestedByName; }
    public void setSuggestedByName(String suggestedByName) { this.suggestedByName = suggestedByName; }
}
