package com.example.btlandroid.Domain;

public class Message {
    private String id;
    private String senderId;
    private String receiverId;
    private String content;
    private long timestamp;
    private boolean isImage;

    public Message() {} // Required for Firebase

    public Message(String id, String senderId, String receiverId, String content, long timestamp) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.timestamp = timestamp;
        this.isImage = false;
    }

    public Message(String id, String senderId, String receiverId, String content, long timestamp, boolean isImage) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.timestamp = timestamp;
        this.isImage = isImage;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isImage() { return isImage; }
    public void setImage(boolean image) { isImage = image; }
}
