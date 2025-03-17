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
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: // Tất cả
                        loadOrders("all");
                        break;
                    case 1: // Đang xử lý
                        loadOrders("processing");
                        break;
                    case 2: // Đang giao
                        loadOrders("shipping");
                        break;
                    case 3: // Hoàn thành
                        loadOrders("completed");
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void loadOrders(String filter) {
        binding.progressBar.setVisibility(View.VISIBLE);
        currentFilter = filter;
        
        Query query;
        
        // Thiết lập query dựa trên filter
        switch (filter) {
            case "processing":
                query = ordersRef.orderByChild("status").equalTo("Đang xử lý");
                break;
            case "shipping":
                query = ordersRef.orderByChild("status").equalTo("Đang giao hàng");
                break;
            case "completed":
                query = ordersRef.orderByChild("status").equalTo("Đã giao hàng");
                break;
            case "all":
            default:
                query = ordersRef;
                break;
        }
        
        query.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                binding.progressBar.setVisibility(View.GONE);
                List<OrderDomain> orderList = new ArrayList<>();
                
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    // Kiểm tra nếu dữ liệu là một ArrayList
                    if (snapshot.getValue() instanceof ArrayList) {
                        // Xử lý với ArrayList
                        ArrayList<Object> arrayData = (ArrayList<Object>) snapshot.getValue();
                        // Tạo đối tượng Order từ dữ liệu ArrayList
                        OrderDomain order = convertArrayToOrder(arrayData, snapshot.getKey());
                        if (order != null) {
                            orderList.add(order);
                        }
                    } else {
                        try {
                            // Nếu dữ liệu là Map, có thể thử dùng cách tiếp cận thông thường
                            Map<String, Object> orderMap = (Map<String, Object>) snapshot.getValue();
                            OrderDomain order = convertMapToOrder(orderMap, snapshot.getKey());
                            if (order != null) {
                                orderList.add(order);
                            }
                        } catch (Exception e) {
                            Log.e("ORDER_CONVERT", "Lỗi khi chuyển đổi dữ liệu: " + e.getMessage());
                        }
                    }
                }
                
                if (orderList.isEmpty()) {
                    binding.emptyTextView.setVisibility(View.VISIBLE);
                } else {
                    binding.emptyTextView.setVisibility(View.GONE);
                    
                    // Sắp xếp đơn hàng từ mới đến cũ (dựa trên timestamp)
                    orderList.sort((o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                    
                    // Cập nhật adapter với dữ liệu mới
                    orderAdapter.setOrderList(orderList);
                    orderAdapter.notifyDataSetChanged();
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(OrderManagementActivity.this, "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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
