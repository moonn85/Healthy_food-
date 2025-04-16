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

import com.example.btlandroid.R; 
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
                // Firebase chỉ cho phép một phương thức orderByChild trong một truy vấn
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
                // Sắp xếp ngay từ Firebase để đảm bảo dữ liệu lấy về đã được sắp xếp
                // Với truy vấn orderByChild("timestamp") Firebase sẽ trả về theo thứ tự tăng dần
                query = ordersRef.orderByChild("timestamp");
                break;
        }

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<OrderDomain> newOrderList = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        // Đọc thủ công từng trường dữ liệu thay vì dùng getValue(OrderDomain.class)
                        String orderId = snapshot.getKey();
                        
                        OrderDomain order = new OrderDomain();
                        order.setOrderId(orderId);
                        
                        // Đảm bảo lấy đúng giá trị timestamp
                        if (snapshot.hasChild("timestamp")) {
                            Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                            // Log để kiểm tra giá trị timestamp
                            Log.d("ORDER_TIMESTAMP", "OrderID: " + orderId + ", Timestamp: " + timestamp);
                            order.setTimestamp(timestamp != null ? timestamp : 0);
                        } else {
                            // Nếu không có timestamp, set giá trị mặc định là 0
                            order.setTimestamp(0L);
                        }
                        
                        // Lấy các trường dữ liệu khác...
                        if (snapshot.hasChild("userId")) {
                            order.setUserId(snapshot.child("userId").getValue(String.class));
                        }
                        
                        if (snapshot.hasChild("status")) {
                            order.setStatus(snapshot.child("status").getValue(String.class));
                        }
                        
                        if (snapshot.hasChild("totalAmount")) {
                            Double amount = snapshot.child("totalAmount").getValue(Double.class);
                            order.setTotalAmount(amount != null ? amount : 0);
                        }
                        
                        // Xử lý các trường khác tương tự...
                        
                        // Xử lý items map
                        if (snapshot.hasChild("items") || snapshot.hasChild("cartItems")) {
                            // ...existing code...
                        }
                        
                        newOrderList.add(order);
                    } catch (Exception e) {
                        Log.e("ORDER_CONVERT", "Error converting order: " + e.getMessage(), e);
                    }
                }

                // Sắp xếp theo timestamp giảm dần - đơn hàng mới nhất lên đầu
                newOrderList.sort((o1, o2) -> {
                    if (o1 == null || o2 == null) return 0;
                    // Log để kiểm tra thứ tự sắp xếp
                    Log.d("ORDER_SORT", "Compare: " + o1.getOrderId() + "(" + o1.getTimestamp() + ") vs " 
                        + o2.getOrderId() + "(" + o2.getTimestamp() + ")");
                    return Long.compare(o2.getTimestamp(), o1.getTimestamp());
                });

                // Log danh sách sau khi sắp xếp
                for (OrderDomain order : newOrderList) {
                    Log.d("ORDER_LIST_SORTED", "ID: " + order.getOrderId() + ", Timestamp: " + order.getTimestamp());
                }

                // Update adapter với danh sách đã sắp xếp
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
                
            }
            
            return order;
        } catch (Exception e) {
            Log.e("ORDER_CONVERT", "Lỗi khi chuyển đổi từ Map: " + e.getMessage());
            return null;
        }
    }
}
