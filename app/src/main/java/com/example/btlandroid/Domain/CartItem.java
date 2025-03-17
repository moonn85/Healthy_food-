package com.example.btlandroid.Domain;

import java.io.Serializable;

public class CartItem implements Serializable {
    private Object id;
    private String title;
    private double price;
    private int quantity;
    private String imagePath;
    private double totalPrice;
    private double itemTotal;
    private String picUrl;
    private double star;

    public CartItem() {
        // Constructor rỗng cần thiết cho Firebase
    }
    
    // Thêm constructor với các tham số cần thiết cho DetailActivity
    public CartItem(int id, String title, double price, String imagePath, int quantity) {
        this.id = id;
        this.title = title;
        this.price = price;
        this.imagePath = imagePath;
        this.quantity = quantity;
        // Tính toán tổng tiền luôn
        this.totalPrice = price * quantity;
        this.itemTotal = price * quantity;
    }

    // Có thể thêm constructor khác với Object id thay vì int
    public CartItem(Object id, String title, double price, String imagePath, int quantity) {
        this.id = id;
        this.title = title;
        this.price = price;
        this.imagePath = imagePath;
        this.quantity = quantity;
        // Tính toán tổng tiền luôn
        this.totalPrice = price * quantity;
        this.itemTotal = price * quantity;
    }

    // Getters and setters
    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public double getItemTotal() {
        // Trả về itemTotal nếu có, nếu không thì trả về totalPrice
        return (itemTotal > 0) ? itemTotal : totalPrice;
    }

    public void setItemTotal(double itemTotal) {
        this.itemTotal = itemTotal;
    }

    public String getPicUrl() {
        return picUrl;
    }

    public void setPicUrl(String picUrl) {
        this.picUrl = picUrl;
    }

    public double getStar() {
        return star;
    }

    public void setStar(double star) {
        this.star = star;
    }
}
