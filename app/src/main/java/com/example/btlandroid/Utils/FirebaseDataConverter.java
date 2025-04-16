package com.example.btlandroid.Utils;

import com.example.btlandroid.Domain.CartItem;
import com.google.firebase.database.DataSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseDataConverter {
    
    /**
     * Chuyển đổi an toàn từ DataSnapshot sang đối tượng CartItem
     * @param snapshot DataSnapshot từ Firebase
     * @return Đối tượng CartItem hoặc null nếu chuyển đổi thất bại
     */
    public static CartItem toCartItem(DataSnapshot snapshot) {
        try {
            // Thử chuyển đổi trực tiếp trước
            return snapshot.getValue(CartItem.class);
        } catch (Exception e) {
            try {
                // Nếu chuyển đổi trực tiếp thất bại, thử chuyển đổi thủ công
                Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                if (map == null) return null;
                
                CartItem item = new CartItem();
                
                // Lấy từng trường một cách an toàn với chuyển đổi kiểu dữ liệu phù hợp
                if (map.containsKey("itemTotal")) {
                    Object totalObj = map.get("itemTotal");
                    if (totalObj instanceof Long) {
                        item.setItemTotal(((Long) totalObj).doubleValue());
                    } else if (totalObj instanceof Double) {
                        item.setItemTotal((Double) totalObj);
                    }
                }
                
                if (map.containsKey("star")) {
                    Object starObj = map.get("star");
                    if (starObj instanceof Long) {
                        item.setStar(((Long) starObj).intValue());
                    } else if (starObj instanceof Integer) {
                        item.setStar((Integer) starObj);
                    }
                }
                
                // Thêm các chuyển đổi trường khác nếu cần
                
                return item;
            } catch (Exception ex) {
                return null;
            }
        }
    }
    
    /**
     * Chuyển đổi an toàn từ DataSnapshot chứa ArrayList sang danh sách các CartItem
     * @param snapshot DataSnapshot từ Firebase
     * @return Danh sách các đối tượng CartItem
     */
    public static List<CartItem> toCartItemList(DataSnapshot snapshot) {
        List<CartItem> items = new ArrayList<>();
        
        try {
            if (snapshot.getValue() instanceof ArrayList) {
                ArrayList<Object> list = (ArrayList<Object>) snapshot.getValue();
                for (Object obj : list) {
                    if (obj instanceof Map) {
                        Map<String, Object> map = (Map<String, Object>) obj;
                        CartItem item = new CartItem();
                        
                        // Ánh xạ thủ công các trường với chuyển đổi kiểu phù hợp
                        // Thêm code để ánh xạ các trường từ map sang CartItem
                        
                        items.add(item);
                    }
                }
            } else {
                // Xử lý như danh sách các phần tử con thông thường
                for (DataSnapshot child : snapshot.getChildren()) {
                    CartItem item = toCartItem(child);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return items;
    }
}
