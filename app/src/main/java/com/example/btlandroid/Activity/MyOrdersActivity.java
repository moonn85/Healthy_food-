package com.example.btlandroid.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btlandroid.Adapter.OrderAdapter;
import com.example.btlandroid.Domain.CartItem;
import com.example.btlandroid.Domain.OrderDomain;
import com.example.btlandroid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class MyOrdersActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private OrderAdapter orderAdapter;
    private List<OrderDomain> orderList;
    private ScrollView scrollView;
    private LinearLayout emptyLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_orders);

        initView();
        loadOrders();
    }

    private void initView() {
        recyclerView = findViewById(R.id.ordersRecyclerView);
        scrollView = findViewById(R.id.scrollView);
        emptyLayout = findViewById(R.id.emptyLayout);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        orderList = new ArrayList<>();
        orderAdapter = new OrderAdapter(orderList);
        recyclerView.setAdapter(orderAdapter);
    }

    private void loadOrders() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để xem đơn hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("Orders");
        
        // Truy vấn đơn hàng theo userId
        ordersRef.orderByChild("userId").equalTo(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                orderList.clear();
                
                // Duyệt qua từng đơn hàng
                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    try {
                        // Không sử dụng snapshot.getValue(OrderDomain.class) trực tiếp
                        // Thay vào đó, chuyển đổi dữ liệu theo cách thủ công
                        String orderId = orderSnapshot.getKey();
                        
                        // Tạo đối tượng OrderDomain mới
                        OrderDomain order = new OrderDomain();
                        order.setOrderId(orderId);
                        
                        // Lấy các trường dữ liệu từ snapshot
                        if (orderSnapshot.hasChild("userId")) {
                            order.setUserId(orderSnapshot.child("userId").getValue(String.class));
                        }
                        
                        if (orderSnapshot.hasChild("userName")) {
                            order.setUserName(orderSnapshot.child("userName").getValue(String.class));
                        }
                        
                        if (orderSnapshot.hasChild("userPhone")) {
                            order.setUserPhone(orderSnapshot.child("userPhone").getValue(String.class));
                        }
                        
                        if (orderSnapshot.hasChild("userEmail")) {
                            order.setUserEmail(orderSnapshot.child("userEmail").getValue(String.class));
                        }
                        
                        if (orderSnapshot.hasChild("address")) {
                            order.setAddress(orderSnapshot.child("address").getValue(String.class));
                        }
                        
                        if (orderSnapshot.hasChild("orderDate")) {
                            order.setOrderDate(orderSnapshot.child("orderDate").getValue(String.class));
                        }
                        
                        if (orderSnapshot.hasChild("totalAmount")) {
                            Double amount = orderSnapshot.child("totalAmount").getValue(Double.class);
                            order.setTotalAmount(amount != null ? amount : 0);
                        }
                        
                        if (orderSnapshot.hasChild("status")) {
                            order.setStatus(orderSnapshot.child("status").getValue(String.class));
                        }
                        
                        if (orderSnapshot.hasChild("paymentMethod")) {
                            order.setPaymentMethod(orderSnapshot.child("paymentMethod").getValue(String.class));
                        }
                        
                        if (orderSnapshot.hasChild("note")) {
                            order.setNote(orderSnapshot.child("note").getValue(String.class));
                        }
                        
                        if (orderSnapshot.hasChild("timestamp")) {
                            Long timestamp = orderSnapshot.child("timestamp").getValue(Long.class);
                            order.setTimestamp(timestamp != null ? timestamp : 0);
                        }
                        
                        // Xử lý đặc biệt cho trường items vì có thể là ArrayList hoặc Map
                        if (orderSnapshot.hasChild("items") || orderSnapshot.hasChild("cartItems")) {
                            DataSnapshot itemsSnapshot = orderSnapshot.hasChild("cartItems") ? 
                                orderSnapshot.child("cartItems") : orderSnapshot.child("items");
                            
                            Map<String, CartItem> itemsMap = new HashMap<>();
                            
                            for (DataSnapshot itemSnapshot : itemsSnapshot.getChildren()) {
                                try {
                                    CartItem item = new CartItem();
                                    
                                    // Lấy các trường dữ liệu của CartItem
                                    if (itemSnapshot.hasChild("productId")) {
                                        item.setProductId(itemSnapshot.child("productId").getValue());
                                    } else if (itemSnapshot.hasChild("id")) {
                                        item.setProductId(itemSnapshot.child("id").getValue());
                                    }
                                    
                                    if (itemSnapshot.hasChild("title")) {
                                        item.setTitle(itemSnapshot.child("title").getValue(String.class));
                                    }
                                    
                                    if (itemSnapshot.hasChild("price")) {
                                        Double price = itemSnapshot.child("price").getValue(Double.class);
                                        item.setPrice(price != null ? price : 0);
                                    }
                                    
                                    if (itemSnapshot.hasChild("quantity")) {
                                        Integer quantity = itemSnapshot.child("quantity").getValue(Integer.class);
                                        item.setQuantity(quantity != null ? quantity : 0);
                                    }
                                    
                                    if (itemSnapshot.hasChild("picUrl") || itemSnapshot.hasChild("imagePath")) {
                                        String imageUrl = itemSnapshot.hasChild("picUrl") ? 
                                            itemSnapshot.child("picUrl").getValue(String.class) : 
                                            itemSnapshot.child("imagePath").getValue(String.class);
                                        item.setPicUrl(imageUrl);
                                    }
                                    
                                    // Thêm vào Map với key là productId
                                    String itemId = item.getProductId();
                                    if (itemId != null) {
                                        itemsMap.put(itemId, item);
                                    }
                                } catch (Exception e) {
                                    Log.e("CART_ITEM_CONVERT", "Lỗi chuyển đổi CartItem: " + e.getMessage());
                                }
                            }
                            
                            order.setItems(itemsMap);
                        }
                        
                        orderList.add(order);
                    } catch (Exception e) {
                        Log.e("ORDER_CONVERT", "Lỗi chuyển đổi đơn hàng: " + e.getMessage());
                    }
                }
                
                // Sắp xếp theo thời gian giảm dần (mới nhất lên đầu)
                orderList.sort((o1, o2) -> {
                    if (o1 == null || o2 == null) return 0;
                    return Long.compare(o2.getTimestamp(), o1.getTimestamp());
                });
                
                // Hiển thị message phù hợp khi không có đơn hàng
                if (orderList.isEmpty()) {
                    emptyLayout.setVisibility(View.VISIBLE);
                    scrollView.setVisibility(View.GONE);
                } else {
                    emptyLayout.setVisibility(View.GONE);
                    scrollView.setVisibility(View.VISIBLE);
                }
                
                // Cập nhật adapter
                orderAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MyOrdersActivity.this, "Lỗi: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
