package com.example.btlandroid.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.btlandroid.Adapter.CartAdapter;
import com.example.btlandroid.Domain.CartItem;
import com.example.btlandroid.R;
import com.example.btlandroid.Utils.BottomNavigationUtils; 
import com.example.btlandroid.Utils.ToastUtils;
import com.example.btlandroid.databinding.ActivityCartBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

// để hiển thị giỏ hàng của người dùng trong ứng dụng Android
// sử dụng Firebase Realtime Database để lưu trữ thông tin giỏ hàng
public class CartActivity extends AppCompatActivity implements CartAdapter.OnCartUpdateListener {

    private ActivityCartBinding binding;
    private List<CartItem> cartItems;
    private CartAdapter cartAdapter;
    private FirebaseUser currentUser;
    private DatabaseReference cartRef;
    private DecimalFormat formatter = new DecimalFormat("#,###");
    private double totalAmount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Kiểm tra người dùng đã đăng nhập chưa
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            ToastUtils.showToast(this, "Vui lòng đăng nhập để xem giỏ hàng");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Khởi tạo RecyclerView
        cartItems = new ArrayList<>();
        cartAdapter = new CartAdapter(cartItems, this, this);
        binding.cartRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.cartRecyclerView.setAdapter(cartAdapter);

        // Tham chiếu đến giỏ hàng của người dùng hiện tại
        cartRef = FirebaseDatabase.getInstance().getReference("Cart").child(currentUser.getUid());

        // Tải dữ liệu giỏ hàng
        loadCartItems();

        // Xử lý nút quay lại
        binding.backBtn.setOnClickListener(v -> finish());

        // Xử lý nút xóa tất cả
        binding.clearCartBtn.setOnClickListener(v -> showClearCartConfirmation());

        // Xử lý nút thanh toán
        binding.checkoutButton.setOnClickListener(v -> {
            if (cartItems.isEmpty()) {
                ToastUtils.showToast(this, "Giỏ hàng của bạn đang trống");
                return;
            }
            
            Intent intent = new Intent(CartActivity.this, CheckoutActivity.class);
            intent.putExtra("totalAmount", totalAmount);
            startActivity(intent);
        });

        // Thiết lập sự kiện cho thanh điều hướng dưới cùng
        BottomNavigationUtils.setupBottomNavigation(this, R.id.cartNav);
    }
    // Phương thức này sẽ được gọi khi giỏ hàng thay đổi từ adapter
    private void loadCartItems() {
        binding.progressBar.setVisibility(View.VISIBLE);

    
        cartRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                cartItems.clear();
                totalAmount = 0;

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    CartItem item = dataSnapshot.getValue(CartItem.class);
                    if (item != null) {
                        // Đảm bảo totalPrice được tính đúng từ giá và số lượng
                        item.setTotalPrice(item.getPrice() * item.getQuantity());
                        cartItems.add(item);
                        totalAmount += item.getTotalPrice();
                    }
                }

                cartAdapter.notifyDataSetChanged();
                binding.progressBar.setVisibility(View.GONE);

                // Cập nhật tổng tiền
                binding.totalPriceText.setText(formatter.format(totalAmount) + " đ");

                // Hiển thị thông báo nếu giỏ hàng trống
                if (cartItems.isEmpty()) {
                    binding.emptyTextView.setVisibility(View.VISIBLE);
                } else {
                    binding.emptyTextView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                ToastUtils.showToast(CartActivity.this, "Lỗi: " + error.getMessage());
            }
        });
    }

    private void showClearCartConfirmation() {
        if (cartItems.isEmpty()) {
            ToastUtils.showToast(this, "Giỏ hàng đã trống");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Xóa giỏ hàng");
        builder.setMessage("Bạn có chắc chắn muốn xóa tất cả sản phẩm trong giỏ hàng?");

        builder.setPositiveButton("Xóa", (dialog, which) -> {
            cartRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    cartItems.clear();
                    cartAdapter.notifyDataSetChanged();
                    binding.totalPriceText.setText("0 đ");
                    binding.emptyTextView.setVisibility(View.VISIBLE);
                    ToastUtils.showToast(CartActivity.this, "Đã xóa giỏ hàng");
                } else {
                    ToastUtils.showToast(CartActivity.this, "Không thể xóa giỏ hàng");
                }
            });
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    @Override
    public void onCartUpdated() {
        // Phương thức này sẽ được gọi khi giỏ hàng thay đổi từ adapter
        recalculateTotalAmount();
    }

    private void recalculateTotalAmount() {
        totalAmount = 0;
        for (CartItem item : cartItems) {
            // Đảm bảo tính tổng giá bằng giá sản phẩm nhân với số lượng
            double itemTotal = item.getPrice() * item.getQuantity();
            item.setTotalPrice(itemTotal);
            totalAmount += itemTotal;
        }
        binding.totalPriceText.setText(formatter.format(totalAmount) + " đ");

        if (cartItems.isEmpty()) {
            binding.emptyTextView.setVisibility(View.VISIBLE);
        } else {
            binding.emptyTextView.setVisibility(View.GONE);
        }
    }
}
