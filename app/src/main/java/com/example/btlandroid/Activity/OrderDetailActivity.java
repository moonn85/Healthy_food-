package com.example.btlandroid.Activity;

import android.app.ProgressDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.example.btlandroid.Domain.CartItem;
import com.example.btlandroid.Domain.OrderDomain;
import com.example.btlandroid.R;
import com.example.btlandroid.databinding.ActivityOrderDetailBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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

            // Xử lý hiển thị các nút dựa trên trạng thái
            binding.confirmBtn.setVisibility(View.GONE);
            binding.shippingBtn.setVisibility(View.GONE);
            binding.deliveredBtn.setVisibility(View.GONE);
            binding.cancelBtn.setVisibility(View.GONE);

            switch (order.getStatus()) {
                case "Chờ xác nhận":
                    binding.confirmBtn.setVisibility(View.VISIBLE);
                    binding.cancelBtn.setVisibility(View.VISIBLE);
                    break;
                case "Đã xác nhận":
                    binding.shippingBtn.setVisibility(View.VISIBLE);
                    binding.cancelBtn.setVisibility(View.VISIBLE);
                    break;
                case "Đang giao hàng":
                    binding.deliveredBtn.setVisibility(View.VISIBLE);
                    binding.cancelBtn.setVisibility(View.VISIBLE);
                    break;
                case "Đã giao hàng":
                case "Đã hủy":
                    binding.adminActionsContainer.setVisibility(View.GONE);
                    break;
            }

            // Thêm sự kiện click cho các nút
            binding.confirmBtn.setOnClickListener(v -> {
                order.setStatus("Đã xác nhận");
                updateOrderStatus(order);
            });

            binding.shippingBtn.setOnClickListener(v -> {
                order.setStatus("Đang giao hàng");
                updateOrderStatus(order);
            });

            binding.deliveredBtn.setOnClickListener(v -> {
                order.setStatus("Đã giao hàng"); 
                updateOrderStatus(order);
            });

            binding.cancelBtn.setOnClickListener(v -> {
                showCancelConfirmDialog(order);
            });
        }
    }

    private void showCancelConfirmDialog(OrderDomain order) {
        new AlertDialog.Builder(this)
            .setTitle("Xác nhận hủy đơn")
            .setMessage("Bạn có chắc chắn muốn hủy đơn hàng này?")
            .setPositiveButton("Hủy đơn", (dialog, which) -> {
                updateOrderStatus(order, "Đã hủy");
            })
            .setNegativeButton("Không", null)
            .show();
    }

    private void updateOrderStatus(OrderDomain order, String newStatus) {
        // Replace ProgressDialog with MaterialAlertDialog
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(this)
                .setView(R.layout.dialog_progress)
                .setCancelable(false)
                .create(); // Add create() before show()
        progressDialog.show();

        DatabaseReference orderRef = FirebaseDatabase.getInstance()
            .getReference("Orders")
            .child(order.getOrderId());

        orderRef.child("status").setValue(newStatus)
            .addOnSuccessListener(aVoid -> {
                progressDialog.dismiss();
                binding.statusTextView.setText(newStatus);
                setStatusColor(binding.statusTextView, newStatus);
                setupAdminActions(order); // Cập nhật lại trạng thái các nút
                Toast.makeText(this, "Cập nhật trạng thái thành công", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void updateOrderStatus(OrderDomain order) {
        // Replace ProgressDialog with MaterialAlertDialog
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(this)
                .setView(R.layout.dialog_progress)
                .setCancelable(false)
                .create(); // Add create() before show()
        progressDialog.show();

        // Cập nhật trạng thái trong Firebase
        DatabaseReference orderRef = FirebaseDatabase.getInstance()
                .getReference("Orders")
                .child(order.getOrderId());

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", order.getStatus());
        updates.put("lastUpdated", System.currentTimeMillis());

        orderRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    // Cập nhật UI
                    binding.statusTextView.setText(order.getStatus());
                    setStatusColor(binding.statusTextView, order.getStatus());
                    setupAdminActions(order);
                    Toast.makeText(this, "Cập nhật trạng thái thành công", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
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
                    
                    // Handle items as Map
                    if (snapshot.hasChild("items")) {
                        Map<String, CartItem> itemsMap = new HashMap<>();
                        for (DataSnapshot itemSnapshot : snapshot.child("items").getChildren()) {
                            String key = itemSnapshot.getKey();
                            CartItem item = itemSnapshot.getValue(CartItem.class);
                            if (key != null && item != null) {
                                itemsMap.put(key, item);
                            }
                        }
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
                    Log.e("ORDER_DETAIL", "Error loading order: " + e.getMessage());
                    Toast.makeText(OrderDetailActivity.this, 
                        "Lỗi khi tải thông tin đơn hàng", Toast.LENGTH_SHORT).show();
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
        int color;
        switch (status) {
            case "Chờ xác nhận":
                color = getResources().getColor(R.color.grey, getTheme());
                break;
            case "Đã xác nhận":
                color = getResources().getColor(R.color.blue, getTheme());
                break;
            case "Đang giao hàng":
                color = getResources().getColor(R.color.yellow, getTheme());
                break;
            case "Đã giao hàng":
                color = getResources().getColor(R.color.green, getTheme());
                break;
            case "Đã hủy":
                color = getResources().getColor(R.color.red, getTheme());
                break;
            default:
                color = getResources().getColor(R.color.grey, getTheme());
                break;
        }
        statusTextView.setBackgroundTintList(ColorStateList.valueOf(color));
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
        
        Map<String, CartItem> items = order.getCartItems();
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
