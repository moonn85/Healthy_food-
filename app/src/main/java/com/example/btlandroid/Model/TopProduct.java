package com.example.btlandroid.Model;

public class TopProduct {
    private String productId;
    private String productName;
    private String imageUrl;
    private int quantitySold;
    private double revenue;

    public TopProduct() {
        // Empty constructor needed for Firebase
    }

    public TopProduct(String productId, String productName, String imageUrl, int quantitySold, double revenue) {
        this.productId = productId;
        this.productName = productName;
        this.imageUrl = imageUrl;
        this.quantitySold = quantitySold;
        this.revenue = revenue;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getQuantitySold() {
        return quantitySold;
    }

    public void setQuantitySold(int quantitySold) {
        this.quantitySold = quantitySold;
    }

    public double getRevenue() {
        return revenue;
    }

    public void setRevenue(double revenue) {
        this.revenue = revenue;
    }
}
