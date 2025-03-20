package com.example.btlandroid.Domain;

import java.io.Serializable;

public class FavoriteItem implements Serializable {
    private String productId;
    private String title;
    private double price;
    private String picUrl;
    private String dateAdded;

    // No-args constructor for Firebase
    public FavoriteItem() {
    }

    // Constructor with all fields as parameters
    public FavoriteItem(String productId, String title, double price, String picUrl, String dateAdded) {
        this.productId = productId;
        this.title = title;
        this.price = price;
        this.picUrl = picUrl;
        this.dateAdded = dateAdded;
    }

    // Getters and setters
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getPicUrl() {
        return picUrl;
    }

    public void setPicUrl(String picUrl) {
        this.picUrl = picUrl;
    }

    public String getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(String dateAdded) {
        this.dateAdded = dateAdded;
    }

    // Method to get ID - returns productId for compatibility
    public String getId() {
        return productId;
    }

    // Method to get image path - returns picUrl for compatibility
    public String getImagePath() {
        return picUrl;
    }
}
