package com.example.btlandroid.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.btlandroid.Domain.CartItem;
import com.example.btlandroid.Domain.OrderDomain;
import com.example.btlandroid.R;
import com.example.btlandroid.databinding.ActivityCheckoutBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class CheckoutActivity extends AppCompatActivity {

    private ActivityCheckoutBinding binding;
    private FirebaseUser currentUser;
    private DatabaseReference cartRef, userRef, ordersRef;
    private double totalCartAmount = 0;
    private double shippingFee = 15000;
    private DecimalFormat formatter = new DecimalFormat("#,###");
    private Map<String, CartItem> cartItems = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCheckoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Kiểm tra người dùng đã đăng nhập chưa
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để thanh toán", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Khởi tạo database references
        cartRef = FirebaseDatabase.getInstance().getReference("Cart").child(currentUser.getUid());
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
        ordersRef = FirebaseDatabase.getInstance().getReference("Orders");

        // Nhận dữ liệu từ intent
        if (getIntent() != null) {
            totalCartAmount = getIntent().getDoubleExtra("totalAmount", 0);
        }

        // Hiển thị thông tin giá
        binding.subtotalTextView.setText(formatter.format(totalCartAmount) + " đ");
        binding.shippingFeeTextView.setText(formatter.format(shippingFee) + " đ");
        binding.totalPaymentTextView.setText(formatter.format(totalCartAmount + shippingFee) + " đ");

        // Lấy thông tin người dùng
        loadUserInfo();
        
        // Lấy thông tin giỏ hàng
        loadCartItems();

        // Xử lý nút quay lại
        binding.backBtn.setOnClickListener(v -> finish());

        // Xử lý nút đặt hàng
        binding.placeOrderButton.setOnClickListener(v -> validateAndPlaceOrder());
    }

    private void loadUserInfo() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    binding.nameEditText.setText(name);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CheckoutActivity.this, "Không thể tải thông tin người dùng", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadCartItems() {
        cartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                cartItems.clear();
                
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    CartItem item = dataSnapshot.getValue(CartItem.class);
                    if (item != null) {
                        cartItems.put(String.valueOf(item.getId()), item);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CheckoutActivity.this, "Không thể tải thông tin giỏ hàng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void validateAndPlaceOrder() {
        // Lấy và kiểm tra thông tin đặt hàng
        String name = binding.nameEditText.getText().toString().trim();
        String phone = binding.phoneEditText.getText().toString().trim();
        String address = binding.addressEditText.getText().toString().trim();
        String note = binding.noteEditText.getText().toString().trim();

        if (name.isEmpty()) {
            binding.nameEditText.setError("Vui lòng nhập họ tên");
            return;
        }

        if (phone.isEmpty()) {
            binding.phoneEditText.setError("Vui lòng nhập số điện thoại");
            return;
        }

        if (address.isEmpty()) {
            binding.addressEditText.setError("Vui lòng nhập địa chỉ giao hàng");
            return;
        }

        // Xác định phương thức thanh toán
        RadioButton selectedRadioButton = findViewById(binding.paymentMethodRadioGroup.getCheckedRadioButtonId());
        String paymentMethod = selectedRadioButton.getText().toString();

        // Hiển thị dialog xác nhận
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Xác nhận đặt hàng");
        builder.setMessage("Bạn có chắc chắn muốn đặt hàng với thông tin đã nhập?");

        builder.setPositiveButton("Đặt hàng", (dialog, which) -> {
            placeOrder(name, phone, address, paymentMethod, note);
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void placeOrder(String name, String phone, String address, String paymentMethod, String note) {
        // Tạo ID đơn hàng ngẫu nhiên
        String orderId = "ORDER_" + UUID.randomUUID().toString();
        
        // Lấy ngày giờ hiện tại
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String orderDate = sdf.format(new Date());
        
        // Tạo đối tượng đơn hàng
        OrderDomain order = new OrderDomain();
        order.setOrderId(orderId);
        order.setUserId(currentUser.getUid());
        order.setUserName(name);
        order.setUserPhone(phone);
        order.setUserEmail(currentUser.getEmail());
        order.setAddress(address);
        order.setOrderDate(orderDate);
        order.setTotalAmount(totalCartAmount + shippingFee);
        order.setStatus("Đang xử lý");
        order.setPaymentMethod(paymentMethod);
        order.setItems(cartItems);
        order.setNote(note);
        
        // Lưu đơn hàng vào database
        ordersRef.child(orderId).setValue(order)
                .addOnSuccessListener(aVoid -> {
                    // Xóa giỏ hàng sau khi đặt hàng thành công
                    cartRef.removeValue().addOnCompleteListener(task -> {
                        // Hiển thị thông báo thành công
                        AlertDialog.Builder successBuilder = new AlertDialog.Builder(this);
                        successBuilder.setTitle("Đặt hàng thành công");
                        successBuilder.setMessage("Cảm ơn bạn đã đặt hàng! Đơn hàng của bạn đang được xử lý.");
                        successBuilder.setPositiveButton("OK", (dialog, which) -> {
                            // Quay về trang chủ
                            Intent intent = new Intent(CheckoutActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        });
                        successBuilder.setCancelable(false);
                        successBuilder.show();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CheckoutActivity.this, "Đặt hàng thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
