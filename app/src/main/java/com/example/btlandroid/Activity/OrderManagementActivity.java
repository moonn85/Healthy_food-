package com.example.btlandroid.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.btlandroid.Adapter.OrderAdapter;
import com.example.btlandroid.Domain.OrderDomain;
import com.example.btlandroid.databinding.ActivityOrderManagementBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.example.btlandroid.R;  // Add this import
import com.example.btlandroid.Domain.CartItem;
import java.util.HashMap;
import java.util.Map;

public class OrderManagementActivity extends AppCompatActivity {

    private ActivityOrderManagementBinding binding;
    private DatabaseReference ordersRef;
    private OrderAdapter orderAdapter;
    private List<OrderDomain> orderList;
    private String currentFilter = "all"; // "all", "processing", "shipping", "completed", "cancelled"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo database references
        ordersRef = FirebaseDatabase.getInstance().getReference("Orders");

        // Thiết lập RecyclerView
        orderList = new ArrayList<>();
        orderAdapter = new OrderAdapter(orderList, this, true); // true = isAdmin
        binding.ordersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.ordersRecyclerView.setAdapter(orderAdapter);

        // Xử lý nút quay lại
        binding.backBtn.setOnClickListener(v -> finish());

        // Thiết lập TabLayout
        setupTabLayout();

        // Tải dữ liệu đơn hàng ban đầu (tất cả đơn hàng)
        loadOrders("all");
    }

    private void setupTabLayout() {
        String[] tabs = {"Tất cả", "Chờ xác nhận", "Đang giao", "Hoàn thành", "Đã hủy"};
        String[] statuses = {"all", "pending", "shipping", "completed", "cancelled"};

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String status = statuses[tab.getPosition()];
                loadOrders(status);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Set up SwipeRefreshLayout with color scheme
        binding.swipeRefreshLayout.setColorSchemeColors(
            getResources().getColor(R.color.yellow, getTheme())
        );
        
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            String status = statuses[binding.tabLayout.getSelectedTabPosition()];
            loadOrders(status);
        });
    }

    private void loadOrders(String filter) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.emptyTextView.setVisibility(View.GONE);

        Query query;
        switch (filter) {
            case "pending":
                query = ordersRef.orderByChild("status").equalTo("Chờ xác nhận");
                break;
            case "shipping":
                query = ordersRef.orderByChild("status").equalTo("Đang giao hàng");
                break;
            case "completed":
                query = ordersRef.orderByChild("status").equalTo("Đã giao hàng");
                break;
            case "cancelled":
                query = ordersRef.orderByChild("status").equalTo("Đã hủy");
                break;
            default:
                query = ordersRef.orderByChild("timestamp");
                break;
        }

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<OrderDomain> newOrderList = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        // Use explicit type checking
                        OrderDomain order = snapshot.getValue(OrderDomain.class);
                        if (order != null) {
                            order.setOrderId(snapshot.getKey());
                            
                            // Handle items map with proper type checking
                            if (snapshot.hasChild("items")) {
                                Map<String, CartItem> itemsMap = new HashMap<>();
                                DataSnapshot itemsSnapshot = snapshot.child("items");
                                for (DataSnapshot itemSnapshot : itemsSnapshot.getChildren()) {
                                    CartItem item = itemSnapshot.getValue(CartItem.class);
                                    if (item != null) {
                                        itemsMap.put(itemSnapshot.getKey(), item);
                                    }
                                }
                                order.setItems(itemsMap);
                            }
                            
                            newOrderList.add(order);
                        }
                    } catch (Exception e) {
                        Log.e("ORDER_CONVERT", "Error converting order: " + e.getMessage());
                    }
                }

                // Sort with type-safe comparison
                newOrderList.sort((o1, o2) -> {
                    if (o1 == null || o2 == null) return 0;
                    return Long.compare(o2.getTimestamp(), o1.getTimestamp());
                });

                // Update adapter with the new list
                orderAdapter.setOrderList(newOrderList);

                // Update UI
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefreshLayout.setRefreshing(false);
                binding.emptyTextView.setVisibility(newOrderList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(OrderManagementActivity.this, 
                    "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Thêm hai phương thức mới để xử lý chuyển đổi dữ liệu
    private OrderDomain convertArrayToOrder(ArrayList<Object> arrayData, String orderId) {
        try {
            // Giả sử cấu trúc của mảng là: [userId, items, total, status, timestamp, ...]
            OrderDomain order = new OrderDomain();
            order.setOrderId(orderId);
            
            if (arrayData.size() >= 5) {
                order.setUserId(arrayData.get(0).toString());
                
                // Xử lý danh sách items nếu có
                if (arrayData.get(1) instanceof ArrayList) {
                    ArrayList<Object> items = (ArrayList<Object>) arrayData.get(1);
                    // Xử lý items ở đây...
                }
                
                if (arrayData.get(2) instanceof Number) {
                    order.setTotalAmount(((Number) arrayData.get(2)).doubleValue());
                }
                
                order.setStatus(arrayData.get(3).toString());
                
                if (arrayData.get(4) instanceof Number) {
                    order.setTimestamp(((Number) arrayData.get(4)).longValue());
                }
            }
            
            return order;
        } catch (Exception e) {
            Log.e("ORDER_CONVERT", "Lỗi khi chuyển đổi từ Array: " + e.getMessage());
            return null;
        }
    }

    private OrderDomain convertMapToOrder(Map<String, Object> orderMap, String orderId) {
        try {
            OrderDomain order = new OrderDomain();
            order.setOrderId(orderId);
            
            if (orderMap.containsKey("userId")) {
                order.setUserId(orderMap.get("userId").toString());
            }
            
            if (orderMap.containsKey("totalAmount") && orderMap.get("totalAmount") instanceof Number) {
                order.setTotalAmount(((Number) orderMap.get("totalAmount")).doubleValue());
            }
            
            if (orderMap.containsKey("status")) {
                order.setStatus(orderMap.get("status").toString());
            }
            
            if (orderMap.containsKey("timestamp") && orderMap.get("timestamp") instanceof Number) {
                order.setTimestamp(((Number) orderMap.get("timestamp")).longValue());
            }
            
            // Xử lý danh sách items nếu có
            if (orderMap.containsKey("items") && orderMap.get("items") instanceof List) {
                // Xử lý items...
            }
            
            return order;
        } catch (Exception e) {
            Log.e("ORDER_CONVERT", "Lỗi khi chuyển đổi từ Map: " + e.getMessage());
            return null;
        }
    }
}
