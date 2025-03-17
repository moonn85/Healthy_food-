package com.example.btlandroid.Activity;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btlandroid.Adapter.OrderAdapter;
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
                        // Lấy key của đơn hàng (orderId)
                        String orderId = orderSnapshot.getKey();
                        
                        // Chuyển đổi snapshot thành OrderDomain
                        OrderDomain order = orderSnapshot.getValue(OrderDomain.class);
                        
                        // Đảm bảo đã set ID khi cần
                        if (order != null && order.getOrderId() == null) {
                            order.setOrderId(orderId);
                        }
                        
                        if (order != null) {
                            orderList.add(order);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                
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
