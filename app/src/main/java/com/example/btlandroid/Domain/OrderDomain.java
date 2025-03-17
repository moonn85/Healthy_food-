package com.example.btlandroid.Domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderDomain implements Serializable {
    private String orderId;
    private String userId;
    private String userName;
    private String userPhone;
    private String userAddress;
    private String userEmail;
    private String address;
    private String note;
    private long timestamp;
    private Map<String, CartItem> items;
    private double totalAmount;
    private String orderDate;
    private String status;
    private String paymentMethod;
    private String notes;

    // Empty constructor for Firebase
    public OrderDomain() {
        this.items = new HashMap<>();
        this.status = "Pending"; // Default status
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(Object orderId) {
        if (orderId instanceof Long) {
            this.orderId = String.valueOf(orderId);
        } else if (orderId instanceof String) {
            this.orderId = (String) orderId;
        }
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(Object userId) {
        if (userId instanceof Long) {
            this.userId = String.valueOf(userId);
        } else if (userId instanceof String) {
            this.userId = (String) userId;
        }
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(Object userName) {
        if (userName instanceof Long) {
            this.userName = String.valueOf(userName);
        } else if (userName instanceof String) {
            this.userName = (String) userName;
        }
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(Object userPhone) {
        if (userPhone instanceof Long) {
            this.userPhone = String.valueOf(userPhone);
        } else if (userPhone instanceof String) {
            this.userPhone = (String) userPhone;
        }
    }

    public String getUserAddress() {
        return userAddress;
    }

    public void setUserAddress(Object userAddress) {
        if (userAddress instanceof Long) {
            this.userAddress = String.valueOf(userAddress);
        } else if (userAddress instanceof String) {
            this.userAddress = (String) userAddress;
        }
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(Object userEmail) {
        if (userEmail instanceof Long) {
            this.userEmail = String.valueOf(userEmail);
        } else if (userEmail instanceof String) {
            this.userEmail = (String) userEmail;
        }
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(Object address) {
        if (address instanceof Long) {
            this.address = String.valueOf(address);
        } else if (address instanceof String) {
            this.address = (String) address;
        }
    }

    public String getNote() {
        return note;
    }

    public void setNote(Object note) {
        if (note instanceof Long) {
            this.note = String.valueOf(note);
        } else if (note instanceof String) {
            this.note = (String) note;
        }
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, CartItem> getItems() {
        return items;
    }

    // Xóa setter cũ và thay thế bằng một setter duy nhất
    public void setItems(Object itemsObject) {
        if (itemsObject instanceof Map) {
            // Xử lý khi items là Map
            try {
                @SuppressWarnings("unchecked")
                Map<String, CartItem> itemsMap = (Map<String, CartItem>) itemsObject;
                this.items = itemsMap;
            } catch (Exception e) {
                this.items = new HashMap<>();
            }
        } else if (itemsObject instanceof List) {
            // Xử lý khi items là List/Array
            try {
                @SuppressWarnings("unchecked")
                List<CartItem> itemsList = (List<CartItem>) itemsObject;
                this.items = new HashMap<>();
                
                // Chuyển từ List sang Map với key là index
                for (int i = 0; i < itemsList.size(); i++) {
                    CartItem item = itemsList.get(i);
                    if (item != null) {
                        this.items.put(String.valueOf(i), item);
                    }
                }
            } catch (Exception e) {
                this.items = new HashMap<>();
            }
        } else {
            this.items = new HashMap<>();
        }
        
        calculateTotalAmount();
    }

    public void addItem(String itemId, CartItem item) {
        if (this.items == null) {
            this.items = new HashMap<>();
        }
        this.items.put(itemId, item);
        calculateTotalAmount();
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Object totalAmount) {
        if (totalAmount instanceof Long) {
            this.totalAmount = ((Long) totalAmount).doubleValue();
        } else if (totalAmount instanceof Integer) {
            this.totalAmount = ((Integer) totalAmount).doubleValue();
        } else if (totalAmount instanceof Double) {
            this.totalAmount = (double) totalAmount;
        } else if (totalAmount instanceof String) {
            try {
                this.totalAmount = Double.parseDouble((String) totalAmount);
            } catch (NumberFormatException e) {
                this.totalAmount = 0;
            }
        }
    }

    public String getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Object orderDate) {
        if (orderDate instanceof Long) {
            this.orderDate = String.valueOf(orderDate);
        } else if (orderDate instanceof String) {
            this.orderDate = (String) orderDate;
        }
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(Object status) {
        if (status instanceof Long) {
            Long statusLong = (Long) status;
            // Chuyển đổi số thành trạng thái tương ứng
            switch(statusLong.intValue()) {
                case 0: this.status = "Đang xử lý"; break;
                case 1: this.status = "Đã xác nhận"; break;
                case 2: this.status = "Đang giao hàng"; break;
                case 3: this.status = "Đã giao hàng"; break;
                case 4: this.status = "Đã hủy"; break;
                default: this.status = "Đang xử lý"; break;
            }
        } else if (status instanceof String) {
            this.status = (String) status;
        }
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(Object method) {
        if (method instanceof Long) {
            this.paymentMethod = String.valueOf(method);
        } else if (method instanceof String) {
            this.paymentMethod = (String) method;
        }
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(Object notes) {
        if (notes instanceof Long) {
            this.notes = String.valueOf(notes);
        } else if (notes instanceof String) {
            this.notes = (String) notes;
        }
    }

    // Helper method to calculate total order amount
    private void calculateTotalAmount() {
        this.totalAmount = 0;
        if (items != null) {
            for (CartItem item : items.values()) {
                if (item != null) {
                    // Kiểm tra itemTotal (một số dùng itemTotal, một số dùng totalPrice)
                    double itemPrice = 0;
                    if (item.getItemTotal() > 0) {
                        itemPrice = item.getItemTotal();
                    } else if (item.getTotalPrice() > 0) {
                        itemPrice = item.getTotalPrice();
                    } else {
                        // Tính bằng giá x số lượng nếu không có sẵn tổng tiền
                        itemPrice = item.getPrice() * item.getQuantity();
                    }
                    this.totalAmount += itemPrice;
                }
            }
        }
    }

    public void setItems(Map<String, Object> itemsMap) {
        this.items = new HashMap<>();
        if (itemsMap != null) {
            for (Map.Entry<String, Object> entry : itemsMap.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    Map<String, Object> itemData = (Map<String, Object>) entry.getValue();
                    CartItem item = new CartItem();
                    
                    // Chuyển đổi từng trường dữ liệu
                    if (itemData.get("id") != null) item.setId(itemData.get("id"));
                    if (itemData.get("title") != null) item.setTitle((String) itemData.get("title"));
                    if (itemData.get("price") != null) {
                        if (itemData.get("price") instanceof Number) {
                            item.setPrice(((Number) itemData.get("price")).doubleValue());
                        }
                    }
                    if (itemData.get("quantity") != null) {
                        if (itemData.get("quantity") instanceof Number) {
                            item.setQuantity(((Number) itemData.get("quantity")).intValue());
                        }
                    }
                    if (itemData.get("imagePath") != null) item.setImagePath((String) itemData.get("imagePath"));
                    
                    this.items.put(entry.getKey(), item);
                }
            }
        }
        calculateTotalAmount();
    }
}
