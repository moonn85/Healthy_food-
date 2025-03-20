package com.example.btlandroid.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.btlandroid.Domain.CartItem;
import com.example.btlandroid.Domain.FavoriteItem;
import com.example.btlandroid.Domain.ItemDomain;
import com.example.btlandroid.R;
import com.example.btlandroid.Utils.ToastUtils;
import com.example.btlandroid.databinding.ActivityDetailBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import android.widget.Toast;

public class DetailActivity extends AppCompatActivity {
    private ActivityDetailBinding binding;
    private ItemDomain item;
    private int quantity = 1;
    private FirebaseUser currentUser;
    private boolean isFavorite = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        getIntentExtraData();
        setVariable();
        checkFavoriteStatus();
    }

    private void getIntentExtraData() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("item")) {
            item = (ItemDomain) intent.getSerializableExtra("item");
            if (item != null) {
                // Use newer decimal format pattern
                DecimalFormat formatter = new DecimalFormat("#,##0");
                String formattedPrice = formatter.format(item.getPrice()) + " VND/Kg";

                binding.titleTxt.setText(item.getTitle());
                binding.priceTxt.setText(formattedPrice);
                binding.descriptionTxt.setText(item.getDescription());
                
                // Use newer rating API
                if (binding.ratingBar != null) {
                    binding.ratingBar.setRating((float) item.getStar());
                }

                // Use load with RequestOptions instead of deprecated placeholder
                Glide.with(this)
                    .load(item.getImagePath())
                    .apply(new RequestOptions()
                        .placeholder(R.drawable.light_black_bg)
                        .error(R.drawable.light_black_bg))
                    .into(binding.productImage);
            }
        }
    }

    private void setVariable() {
        // Cập nhật code để thêm sự kiện click cho nút thêm vào giỏ hàng
        binding.addToCartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addToCart();
            }
        });


        binding.favBtn.setOnClickListener(v -> toggleFavorite());
        
        // Thêm sự kiện click cho nút back để quay lại màn hình trước đó
        binding.backBtn.setOnClickListener(v -> finish());

        // Removed references to missing binding elements to fix compilation errors
    }

    private void calculateTotalPrice() {
        double totalPrice = quantity * item.getPrice();
        binding.priceTxt.setText(totalPrice + " đ");
    }

    /**
     * Thêm sản phẩm vào giỏ hàng của người dùng
     */
    private void addToCart() {
        // Kiểm tra đối tượng item có tồn tại không
        if (item == null) {
            ToastUtils.showToast(this, "Không thể tải thông tin sản phẩm. Vui lòng thử lại sau.");
            return;
        }
        
        if (currentUser == null) {
            ToastUtils.showToast(this, "Vui lòng đăng nhập để thêm vào giỏ hàng");
            startActivity(new Intent(DetailActivity.this, LoginActivity.class));
            return;
        }


        binding.addToCartBtn.setEnabled(false);

        // Tạo đối tượng CartItem mới
        CartItem cartItem = new CartItem(
                String.valueOf(item.getId()), // Convert the int id to String for productId
                item.getTitle(),
                item.getPrice(),
                item.getImagePath(),
                quantity
        );

        // Lấy reference đến node giỏ hàng của người dùng
        DatabaseReference cartRef = FirebaseDatabase.getInstance()
                .getReference("Cart")
                .child(currentUser.getUid())
                .child(String.valueOf(item.getId())); // Use getId() instead of getProductId()

        // Kiểm tra xem sản phẩm đã tồn tại trong giỏ hàng chưa
        cartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Nếu sản phẩm đã tồn tại, cập nhật số lượng
                    CartItem existingItem = snapshot.getValue(CartItem.class);
                    if (existingItem != null) {
                        existingItem.setQuantity(existingItem.getQuantity() + quantity);
                        cartRef.setValue(existingItem).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                handleCartResult(task);
                            }
                        });
                    }
                } else {
                    // Nếu sản phẩm chưa tồn tại, thêm mới
                    cartRef.setValue(cartItem).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            handleCartResult(task);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Remove reference to progressBar
                binding.addToCartBtn.setEnabled(true);
                Toast.makeText(DetailActivity.this, "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleCartResult(Task<Void> task) {
        // Remove reference to progressBar
        binding.addToCartBtn.setEnabled(true);
        
        if (task.isSuccessful()) {
            ToastUtils.showToast(DetailActivity.this, "Đã thêm vào giỏ hàng");
        } else {
            ToastUtils.showToast(DetailActivity.this, "Không thể thêm vào giỏ hàng");
        }
    }

    private void checkFavoriteStatus() {
        if (currentUser == null || item == null) return;

        DatabaseReference favoriteRef = FirebaseDatabase.getInstance().getReference("Favorites")
                .child(currentUser.getUid())
                .child(String.valueOf(item.getId())); // Use getId() instead of getProductId()

        favoriteRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isFavorite = snapshot.exists();
                binding.favBtn.setImageResource(isFavorite ? R.drawable.favorite_green : R.drawable.favorite_border);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DetailActivity.this, "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleFavorite() {
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để thêm vào yêu thích", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference favoriteRef = FirebaseDatabase.getInstance().getReference("Favorites")
                .child(currentUser.getUid())
                .child(String.valueOf(item.getId())); // Use getId() instead of getProductId()
        
        if (isFavorite) {
            // Remove from favorites
            favoriteRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    isFavorite = false;
                    binding.favBtn.setImageResource(R.drawable.favorite_border);
                    Toast.makeText(this, "Đã xóa khỏi danh sách yêu thích", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Không thể xóa khỏi danh sách yêu thích", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Modify this part to correctly create the FavoriteItem object
            // First let's create a variable to hold the ID as String
            String productIdStr = String.valueOf(item.getId());
            
            FavoriteItem favoriteItem = new FavoriteItem(
                productIdStr,  // Use the String variable instead of direct conversion
                item.getTitle(),
                item.getPrice(),
                item.getImagePath(),
                String.valueOf(System.currentTimeMillis())
            );
            
            favoriteRef.setValue(favoriteItem).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    isFavorite = true;
                    binding.favBtn.setImageResource(R.drawable.ic_favorite);
                    Toast.makeText(this, "Đã thêm vào danh sách yêu thích", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Không thể thêm vào danh sách yêu thích", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // Thêm phương thức để kiểm tra và khởi tạo lại dữ liệu nếu cần
    @Override
    protected void onResume() {
        super.onResume();
        // Kiểm tra xem item có bị null không và thử tải lại nếu cần
        if (item == null) {
            getIntentExtraData();
        }
    }
}
