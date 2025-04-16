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

import android.widget.ImageView;
import com.bumptech.glide.Glide;

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
        // Hiển thị/ẩn phần hành động của admin dựa trên vai trò
        if (binding.adminActionsContainer != null) {
            binding.adminActionsContainer.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        }
        
        // Hiển thị/ẩn phần hành động của người dùng dựa trên vai trò
        if (binding.userActionsContainer != null) {
            binding.userActionsContainer.setVisibility(isAdmin ? View.GONE : View.VISIBLE);
        }
    }

    private void setupAdminActions(OrderDomain order) {
        if (!isAdmin) {
            // Thiết lập hành động cho người dùng
            setupUserActions(order);
            return;
        }

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
    
    private void setupUserActions(OrderDomain order) {
        if (isAdmin) return;
        
        if (binding.userActionsContainer != null) {
            // Chỉ hiển thị nút hủy đơn hàng khi đơn hàng đang ở trạng thái "Chờ xác nhận" hoặc "Đã xác nhận"
            String status = order.getStatus();
            boolean canCancel = "Chờ xác nhận".equals(status) || "Đã xác nhận".equals(status);
            
            binding.userActionsContainer.setVisibility(canCancel ? View.VISIBLE : View.GONE);
            
            if (canCancel) {
                binding.userCancelBtn.setOnClickListener(v -> {
                    showUserCancelConfirmDialog(order);
                });
            }
        }
    }
    
    private void showUserCancelConfirmDialog(OrderDomain order) {
        new AlertDialog.Builder(this)
            .setTitle("Xác nhận hủy đơn")
            .setMessage("Bạn có chắc chắn muốn hủy đơn hàng này?")
            .setPositiveButton("Hủy đơn", (dialog, which) -> {
                updateOrderStatus(order, "Đã hủy");
            })
            .setNegativeButton("Không", null)
            .show();
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
        // Hiển thị loading
        // ...existing code...
        
        orderRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    try {
                        // Không sử dụng snapshot.getValue(OrderDomain.class) trực tiếp
                        // Thay vào đó, tạo đối tượng OrderDomain và thiết lập thủ công
                        OrderDomain order = new OrderDomain();
                        order.setOrderId(orderId);
                        
                        // Lấy các trường dữ liệu từ snapshot
                        if (snapshot.hasChild("userId")) {
                            order.setUserId(snapshot.child("userId").getValue(String.class));
                        }
                        
                        if (snapshot.hasChild("userName")) {
                            order.setUserName(snapshot.child("userName").getValue(String.class));
                        }
                        
                        if (snapshot.hasChild("userPhone")) {
                            order.setUserPhone(snapshot.child("userPhone").getValue(String.class));
                        }
                        
                        if (snapshot.hasChild("userEmail")) {
                            order.setUserEmail(snapshot.child("userEmail").getValue(String.class));
                        }
                        
                        if (snapshot.hasChild("address")) {
                            order.setAddress(snapshot.child("address").getValue(String.class));
                        }
                        
                        if (snapshot.hasChild("orderDate")) {
                            order.setOrderDate(snapshot.child("orderDate").getValue(String.class));
                        }
                        
                        if (snapshot.hasChild("totalAmount")) {
                            Double amount = snapshot.child("totalAmount").getValue(Double.class);
                            order.setTotalAmount(amount != null ? amount : 0);
                        }
                        
                        if (snapshot.hasChild("status")) {
                            order.setStatus(snapshot.child("status").getValue(String.class));
                        }
                        
                        if (snapshot.hasChild("paymentMethod")) {
                            order.setPaymentMethod(snapshot.child("paymentMethod").getValue(String.class));
                        }
                        
                        if (snapshot.hasChild("note")) {
                            order.setNote(snapshot.child("note").getValue(String.class));
                        }
                        
                        if (snapshot.hasChild("timestamp")) {
                            Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                            order.setTimestamp(timestamp != null ? timestamp : 0);
                        }
                        
                        // Xử lý đặc biệt cho trường items vì có thể là ArrayList hoặc Map
                        if (snapshot.hasChild("items") || snapshot.hasChild("cartItems")) {
                            DataSnapshot itemsSnapshot = snapshot.hasChild("cartItems") ? 
                                snapshot.child("cartItems") : snapshot.child("items");
                            
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
                            order.setCartItems(itemsMap); // Đảm bảo cả hai trường đều được thiết lập
                        }
                        
                        // Hiển thị thông tin đơn hàng
                        displayOrderInfo(order);
                        
                        // Hiển thị thông tin khách hàng
                        displayCustomerInfo(order);
                        
                        // Hiển thị danh sách sản phẩm
                        displayOrderItems(order);
                        
                        // Thiết lập các hành động dành cho admin
                        setupAdminActions(order);
                    } catch (Exception e) {
                        Log.e("ORDER_DETAIL", "Lỗi: " + e.getMessage());
                        Toast.makeText(OrderDetailActivity.this, 
                            "Lỗi khi xử lý dữ liệu đơn hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // ...existing code...
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
        
        // Ghi log để theo dõi
        Log.d("ORDER_ITEMS", "Đang hiển thị sản phẩm trong đơn hàng: " + order.getOrderId());
        
        Map<String, CartItem> items = order.getCartItems();
        double totalAmount = 0;
        
        if (items != null && !items.isEmpty()) {
            Log.d("ORDER_ITEMS", "Số sản phẩm: " + items.size());
            
            for (CartItem item : items.values()) {
                try {
                    if (item == null) {
                        Log.e("ORDER_ITEMS", "Item is null");
                        continue;
                    }
                    
                    View itemView = LayoutInflater.from(this)
                        .inflate(R.layout.item_order_detail, container, false);
                        
                    ImageView productImageView = itemView.findViewById(R.id.productImageView);
                    TextView nameTxt = itemView.findViewById(R.id.productNameTxt);
                    TextView quantityTxt = itemView.findViewById(R.id.productQuantityTxt);
                    TextView priceTxt = itemView.findViewById(R.id.productPriceTxt);
                    
                    // Hiển thị thông tin cơ bản ngay cả khi có lỗi
                    nameTxt.setText(item.getTitle() != null ? item.getTitle() : "Không có tên");
                    quantityTxt.setText("x" + item.getQuantity());
                    
                    // Định dạng và hiển thị giá
                    double itemTotal = item.getPrice() * item.getQuantity();
                    priceTxt.setText(formatter.format(itemTotal) + " đ");
                    
                    // Tải ảnh sản phẩm nếu có
                    String imageUrl = item.getPicUrl();
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Log.d("ORDER_ITEMS", "Đang tải ảnh: " + imageUrl);
                        
                        Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.grey_button_bg)
                            .error(R.drawable.grey_button_bg)
                            .into(productImageView);
                    } else {
                        Log.d("ORDER_ITEMS", "Không có URL ảnh cho sản phẩm: " + item.getTitle());
                    }
                    
                    container.addView(itemView);
                    
                    totalAmount += itemTotal;
                } catch (Exception e) {
                    Log.e("ORDER_ITEMS", "Lỗi khi hiển thị sản phẩm: " + e.getMessage());
                }
            }
        } else {
            Log.e("ORDER_ITEMS", "Không có sản phẩm trong đơn hàng");
            
            // Hiển thị thông báo nếu không có sản phẩm
            TextView emptyView = new TextView(this);
            emptyView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
            emptyView.setText("Không có thông tin sản phẩm");
            emptyView.setTextColor(getResources().getColor(R.color.grey));
            emptyView.setPadding(0, 16, 0, 16);
            container.addView(emptyView);
        }
        
        // Hiển thị tổng tiền
        binding.subtotalTextView.setText(formatter.format(totalAmount) + " đ");
        
        // Sử dụng tổng tiền từ đơn hàng nếu có
        double finalTotal = totalAmount;
        if (order.getTotalAmount() > 0 && totalAmount == 0) {
            finalTotal = order.getTotalAmount() - 15000; // Trừ phí ship
        }
        
        // Giả sử phí ship cố định là 15,000đ
        double shippingFee = 15000;
        binding.shippingFeeTextView.setText(formatter.format(shippingFee) + " đ");
        
        // Tổng thanh toán
        binding.totalPaymentTextView.setText(formatter.format(finalTotal + shippingFee) + " đ");
    }
}
