package com.example.btlandroid.Domain;

public class User {
    private String id;
    private String name;
    private String email;
    private String profileImage;
    private long lastActive; // Thêm trường lastActive

    // Constructor mặc định
    public User() {}

    // Constructor đầy đủ 
    public User(String id, String name, String email, String profileImage, long lastActive) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.profileImage = profileImage;
        this.lastActive = lastActive;
    }

    // Getters & setters hiện có...
    
    public long getLastActive() {
        return lastActive;
    }

    public void setLastActive(long lastActive) {
        this.lastActive = lastActive;
    }

    // Add these getter/setter methods
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}
