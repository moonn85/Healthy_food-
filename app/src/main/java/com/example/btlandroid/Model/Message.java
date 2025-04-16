package com.example.btlandroid.Model;

public class Message {
    private String messageId;
    private String senderId;
    private String receiverId;
    private String content;
    private long timestamp;
    private boolean isSeen;
    private String imageUrl;
    private boolean isRead;  // Thêm field mới

    public Message() {
        // Constructor trống cho Firebase
        this.isRead = false;
    }

    public Message(String messageId, String senderId, String receiverId, String content, long timestamp) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.timestamp = timestamp;
        this.isSeen = false;
        this.imageUrl = null;
        this.isRead = false;
    }

    // Thêm constructor cho tin nhắn hình ảnh
    public Message(String messageId, String senderId, String receiverId, String imageUrl, long timestamp, boolean isImage) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.isSeen = false;
        this.content = isImage ? null : imageUrl;
    }

    // Getters and setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSeen() {
        return isSeen;
    }

    public void setSeen(boolean seen) {
        isSeen = seen;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean hasImage() {
        return imageUrl != null && !imageUrl.isEmpty();
    }

    // Thêm getter/setter mới
    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}
