package com.example.btlandroid.Domain;

public class FavoriteItem {
    private int id;
    private String title;
    private double price;
    private String imagePath;
    private String timestamp;

    // Constructor rỗng cần thiết cho Firebase
    public FavoriteItem() {
    }

    public FavoriteItem(int id, String title, double price, String imagePath, String timestamp) {
        this.id = id;
        this.title = title;
        this.price = price;
        this.imagePath = imagePath;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
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

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
