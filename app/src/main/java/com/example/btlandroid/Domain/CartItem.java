package com.example.btlandroid.Domain;

import java.io.Serializable;

public class CartItem implements Serializable {
    // Original properties with proper Firebase mapping names
    private String productId;
    private String title;
    private double price;
    private String picUrl;
    private int quantity;
    private double totalPrice;
    
    // Additional properties to match Firebase database fields
    private String id;          // For Firebase mapping
    private String imagePath;   // For Firebase mapping

    // Add missing fields that Firebase expects
    private double itemTotal;
    private int star;

    // No-args constructor cho Firebase
    public CartItem() {
    }

    public CartItem(String productId, String title, double price, String picUrl, int quantity) {
        this.productId = productId;
        this.id = productId;    // Set id same as productId
        this.title = title;
        this.price = price;
        this.picUrl = picUrl;
        this.imagePath = picUrl; // Set imagePath same as picUrl
        this.quantity = quantity;
        this.totalPrice = price * quantity;
    }

    // Getters và Setters
    public String getProductId() {
        return productId;
    }

    // Chỉ giữ một phương thức setter cho productId
    public void setProductId(Object productId) {
        if (productId instanceof Long) {
            this.productId = String.valueOf(productId);
        } else if (productId instanceof String) {
            this.productId = (String) productId;
        } else if (productId != null) {
            this.productId = String.valueOf(productId);
        }
        this.id = this.productId; // Đồng bộ với id
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
        // Cập nhật tổng giá khi giá thay đổi
        this.totalPrice = price * quantity;
    }

    public String getPicUrl() {
        return picUrl;
    }

    public void setPicUrl(String picUrl) {
        this.picUrl = picUrl;
        this.imagePath = picUrl; // Keep picUrl and imagePath in sync
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        // Cập nhật tổng giá khi số lượng thay đổi
        this.totalPrice = price * quantity;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    // Phương thức tính tổng giá
    public void updateTotalPrice() {
        this.totalPrice = this.price * this.quantity;
    }
    
    // Additional getters and setters for Firebase mapping
    public String getId() {
        return productId; // Return productId as id
    }

    // Chỉ giữ một phương thức setter cho id
    public void setId(Object id) {
        if (id instanceof Long) {
            this.id = String.valueOf(id);
        } else if (id instanceof String) {
            this.id = (String) id;
        } else if (id != null) {
            this.id = String.valueOf(id);
        }
        this.productId = this.id; // Đồng bộ với productId
    }

    public String getImagePath() {
        return picUrl; // Return picUrl as imagePath
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
        this.picUrl = imagePath; // Keep picUrl and imagePath in sync
    }

    // Add getters and setters for new fields
    public double getItemTotal() {
        return itemTotal;
    }
    
    public void setItemTotal(double itemTotal) {
        this.itemTotal = itemTotal;
    }
    
    public int getStar() {
        return star;
    }
    
    public void setStar(int star) {
        this.star = star;
    }
}
