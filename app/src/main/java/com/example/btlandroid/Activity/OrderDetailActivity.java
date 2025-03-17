package com.example.btlandroid.Activity;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.btlandroid.Domain.CartItem;
import com.example.btlandroid.Domain.OrderDomain;
import com.example.btlandroid.R;
import com.example.btlandroid.databinding.ActivityOrderDetailBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class OrderDetailActivity extends AppCompatActivity {

    private ActivityOrderDetailBinding binding;
    private String orderId;
    private DatabaseReference orderRef;
    private FirebaseUser currentUser;
    private DecimalFormat formatter = new DecimalFormat("#,###");
    private boolean isAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Kiểm tra xem có phải admin không (có thể kiểm tra từ Firebase)
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference adminRef = FirebaseDatabase.getInstance().getReference("Admins")
                    .child(currentUser.getUid());
            adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    isAdmin = snapshot.exists();
                    setupAdminActions();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }

        // Lấy orderId từ Intent
        if (getIntent() != null && getIntent().hasExtra("orderId")) {
            orderId = getIntent().getStringExtra("orderId");
            orderRef = FirebaseDatabase.getInstance().getReference("Orders").child(orderId);

            // Tải thông tin đơn hàng
            loadOrderDetails();
        } else {
            Toast.makeText(this, "Không tìm thấy thông tin đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Xử lý nút quay lại
        binding.backBtn.setOnClickListener(v -> finish());
    }

    private void setupAdminActions() {
        if (binding.adminActionsContainer != null) {  // Use correct layout ID
            binding.adminActionsContainer.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        }
    }

    private void setupAdminActions(OrderDomain order) {
        if (!isAdmin) return;

        if (binding.adminActionsContainer != null) {
            binding.adminActionsContainer.setVisibility(View.VISIBLE);
            
            // Setup admin action buttons
            binding.confirmBtn.setOnClickListener(v -> updateOrderStatus(order, "Đã xác nhận"));
            binding.shippingBtn.setOnClickListener(v -> updateOrderStatus(order, "Đang giao hàng")); 
            binding.deliveredBtn.setOnClickListener(v -> updateOrderStatus(order, "Đã giao hàng"));
            binding.cancelBtn.setOnClickListener(v -> updateOrderStatus(order, "Đã hủy"));
        }
    }

    private void updateOrderStatus(OrderDomain order, String newStatus) {
        orderRef.child("status").setValue(newStatus)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Cập nhật trạng thái thành công", Toast.LENGTH_SHORT).show();
                binding.statusTextView.setText(newStatus);
                setStatusColor(binding.statusTextView, newStatus);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void loadOrderDetails() {
        orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    OrderDomain order = new OrderDomain();
                    // Manually set fields from snapshot
                    order.setOrderId(snapshot.child("orderId").getValue(String.class));
                    order.setUserId(snapshot.child("userId").getValue(String.class));
                    order.setUserName(snapshot.child("userName").getValue(String.class));
                    order.setUserPhone(snapshot.child("userPhone").getValue(String.class));
                    order.setUserEmail(snapshot.child("userEmail").getValue(String.class));
                    order.setAddress(snapshot.child("address").getValue(String.class));
                    order.setOrderDate(snapshot.child("orderDate").getValue(String.class));
                    order.setStatus(snapshot.child("status").getValue(String.class));
                    order.setPaymentMethod(snapshot.child("paymentMethod").getValue(String.class));
                    order.setNote(snapshot.child("note").getValue(String.class));
                    
                    // Handle items separately
                    if (snapshot.hasChild("items")) {
                        Map<String, Object> itemsMap = (Map<String, Object>) snapshot.child("items").getValue();
                        order.setItems(itemsMap);
                    }

                    // Display order information
                    displayOrderInfo(order);
                    displayCustomerInfo(order);
                    displayOrderItems(order);
                    
                    if (isAdmin) {
                        binding.adminActionsContainer.setVisibility(View.VISIBLE);
                        setupAdminActions(order);
                    }
                } catch (Exception e) {
                    Toast.makeText(OrderDetailActivity.this, 
                        "Lỗi khi tải thông tin đơn hàng: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(OrderDetailActivity.this, "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayOrderInfo(OrderDomain order) {
        binding.orderIdTextView.setText(order.getOrderId());
        binding.orderDateTextView.setText(order.getOrderDate());
        binding.statusTextView.setText(order.getStatus());
        binding.paymentMethodTextView.setText(order.getPaymentMethod());
        
        // Thiết lập màu sắc cho trạng thái
        setStatusColor(binding.statusTextView, order.getStatus());
    }
    
    private void setStatusColor(TextView statusTextView, String status) {
        switch (status) {
            case "Đang xử lý":
                statusTextView.setBackgroundResource(R.drawable.grey_button_bg);
                break;
            case "Đã xác nhận":
                statusTextView.setBackgroundResource(R.drawable.blue_button_bg);
                break;
            case "Đang giao hàng":
                statusTextView.setTextColor(Color.BLACK);
                statusTextView.setBackgroundResource(R.drawable.yellow_bg);
                break;
            case "Đã giao hàng":
                statusTextView.setTextColor(Color.WHITE);
                statusTextView.setBackgroundResource(R.drawable.green_button_bg);
                break;
            case "Đã hủy":
                statusTextView.setBackgroundResource(R.drawable.red_button_bg);
                break;
            default:
                statusTextView.setBackgroundResource(R.drawable.grey_button_bg);
                break;
        }
    }

    private void displayCustomerInfo(OrderDomain order) {
        binding.nameTextView.setText(order.getUserName());
        binding.phoneTextView.setText(order.getUserPhone());
        binding.emailTextView.setText(order.getUserEmail());
        binding.addressTextView.setText(order.getAddress());
        
        // Hiển thị ghi chú nếu có
        if (order.getNote() != null && !order.getNote().isEmpty()) {
            binding.noteTextView.setText(order.getNote());
        } else {
            binding.noteLayout.setVisibility(View.GONE);
        }
    }

    private void displayOrderItems(OrderDomain order) {
        LinearLayout container = binding.orderItemsContainer;
        container.removeAllViews();
        
        Map<String, CartItem> items = order.getItems();
        double totalAmount = 0;
        
        if (items != null && !items.isEmpty()) {
            for (CartItem item : items.values()) {
                View itemView = LayoutInflater.from(this)
                    .inflate(R.layout.item_order_detail, container, false);
                    
                TextView nameTxt = itemView.findViewById(R.id.productNameTxt);
                TextView quantityTxt = itemView.findViewById(R.id.productQuantityTxt);
                TextView priceTxt = itemView.findViewById(R.id.productPriceTxt);
                
                nameTxt.setText(item.getTitle());
                quantityTxt.setText("x" + item.getQuantity());
                priceTxt.setText(formatter.format(item.getTotalPrice()) + " đ");
                
                container.addView(itemView);
                
                totalAmount += item.getTotalPrice();
            }
        }
        
        // Hiển thị tổng tiền
        binding.subtotalTextView.setText(formatter.format(totalAmount) + " đ");
        
        // Giả sử phí ship cố định là 15,000đ
        double shippingFee = 15000;
        binding.shippingFeeTextView.setText(formatter.format(shippingFee) + " đ");
        
        // Tổng thanh toán
        binding.totalPaymentTextView.setText(formatter.format(totalAmount + shippingFee) + " đ");
    }
}
