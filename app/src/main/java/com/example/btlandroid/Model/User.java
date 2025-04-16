package com.example.btlandroid.Model;

public class User {
    private String id;
    private String name;
    private String email;
    private String profileImage;
    private boolean isAdmin;
    private long lastActive; // Add lastActive field
    private boolean isOnline; // Add online status field

    // Constructor rỗng cần thiết cho Firebase
    public User() {
    }

    public User(String id, String name, String email, String profileImage) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.profileImage = profileImage;
        this.isAdmin = false;
        this.lastActive = System.currentTimeMillis(); // Initialize with current time
        this.isOnline = false; // Default to offline
    }

 
    public User(String id, String name, String email, String profileImage, long lastActive) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.profileImage = profileImage;
        this.isAdmin = false;
        this.lastActive = lastActive;
        this.isOnline = false; 
    }

    public User(String id, String name, String email, String profileImage, boolean isAdmin, long lastActive, boolean isOnline) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.profileImage = profileImage;
        this.isAdmin = isAdmin;
        this.lastActive = lastActive;
        this.isOnline = isOnline;
    }

    // Getters and Setters
    public String getId() {
        return id != null ? id : "";
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email != null ? email : "";
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

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }
    
    // Add getter and setter for lastActive
    public long getLastActive() {
        return lastActive;
    }

    public void setLastActive(long lastActive) {
        this.lastActive = lastActive;
    }
    
    // Add getter and setter for online status
    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }
}
