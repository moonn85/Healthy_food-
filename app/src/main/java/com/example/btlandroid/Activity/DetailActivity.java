package com.example.btlandroid.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

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

// để hiển thị thông tin chi tiết sản phẩm
// sử dụng Firebase để lưu trữ thông tin giỏ hàng và danh sách yêu thích
public class DetailActivity extends AppCompatActivity {
    private ActivityDetailBinding binding;
    private ItemDomain item;
    private int quantity = 1; // Biến lưu số lượng sản phẩm
    private FirebaseUser currentUser;
    private boolean isFavorite = false;
    private ImageButton btnMinus, btnPlus;
    private TextView quantityTxt, totalPriceTxt;
    private DecimalFormat formatter; // Định dạng số để hiển thị giá tiền
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        // Khởi tạo formatter để định dạng tiền tệ
        formatter = new DecimalFormat("#,###");
        
        // Khai báo và khởi tạo các phần tử UI mới
        setupQuantityControls();
        
        getIntentExtraData();
        setVariable();
        checkFavoriteStatus();
    }

    /**
     * Khởi tạo và xử lý các thành phần điều khiển số lượng
     */
    private void setupQuantityControls() {
        btnMinus = findViewById(R.id.btnMinus);
        btnPlus = findViewById(R.id.btnPlus);
        quantityTxt = findViewById(R.id.quantityTxt);
        totalPriceTxt = findViewById(R.id.totalPriceTxt);
        
        // Xử lý sự kiện nút giảm số lượng
        btnMinus.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                updateQuantityAndTotal();
            }
        });
        
        // Xử lý sự kiện nút tăng số lượng
        btnPlus.setOnClickListener(v -> {
            quantity++;
            updateQuantityAndTotal();
        });
    }
    
    /**
     * Cập nhật hiển thị số lượng và tổng tiền
     */
    private void updateQuantityAndTotal() {
        if (quantityTxt != null) {
            quantityTxt.setText(String.valueOf(quantity));
        }
        
        calculateTotalPrice();
    }

    // Nhận dữ liệu từ Intent và hiển thị thông tin sản phẩm
    // Sử dụng DecimalFormat để định dạng giá tiền
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
        
        // Sau khi lấy dữ liệu item, cập nhật tổng tiền ban đầu
        if (item != null) {
            calculateTotalPrice();
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

    /**
     * Tính và hiển thị tổng tiền dựa trên số lượng
     */
    private void calculateTotalPrice() {
        if (item != null && totalPriceTxt != null) {
            double totalPrice = quantity * item.getPrice();
            totalPriceTxt.setText("Tổng: " + formatter.format(totalPrice) + " đ");
        }
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

        // Xử lý chuyển đổi ID sản phẩm từ Long sang String
        String productId;
        Object idObj = item.getId();
        
        if (idObj instanceof Long) {
            productId = String.valueOf(((Long) idObj).longValue());
        } else if (idObj instanceof Integer) {
            productId = String.valueOf(((Integer) idObj).intValue());
        } else {
            productId = String.valueOf(idObj);
        }

        // Tạo đối tượng CartItem với ID đã được xử lý
        CartItem cartItem = new CartItem(
                productId,
                item.getTitle(),
                item.getPrice(),
                item.getImagePath(),
                quantity // Sử dụng số lượng đã chọn
        );

        // Lấy reference đến node giỏ hàng của người dùng
        // SỬA: Sử dụng productId thay vì String.valueOf(item.getId())
        DatabaseReference cartRef = FirebaseDatabase.getInstance()
                .getReference("Cart")
                .child(currentUser.getUid())
                .child(productId); // Sử dụng productId đã được xử lý

        // Kiểm tra xem sản phẩm đã tồn tại trong giỏ hàng chưa
        cartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Thay vì dùng getValue(), lấy các giá trị riêng lẻ để tránh lỗi chuyển đổi
                    try {
                        // Lấy từng thuộc tính riêng lẻ
                        String id = snapshot.child("productId").getValue(String.class);
                        String title = snapshot.child("title").getValue(String.class);
                        Double price = snapshot.child("price").getValue(Double.class);
                        String image = snapshot.child("imagePath").getValue(String.class);
                        Integer currentQuantity = snapshot.child("quantity").getValue(Integer.class);

                        // Tạo đối tượng CartItem mới và thiết lập các thuộc tính
                        CartItem existingItem = new CartItem();
                        existingItem.setProductId(id != null ? id : productId);
                        existingItem.setTitle(title != null ? title : item.getTitle());
                        existingItem.setPrice(price != null ? price : item.getPrice());
                        existingItem.setImagePath(image != null ? image : item.getImagePath());
                        existingItem.setQuantity((currentQuantity != null ? currentQuantity : 0) + quantity);

                        // Cập nhật vào database
                        cartRef.setValue(existingItem).addOnCompleteListener(task -> handleCartResult(task));
                    } catch (Exception e) {
                        // Nếu có lỗi khi parse dữ liệu, thêm mới sản phẩm
                        cartRef.setValue(cartItem).addOnCompleteListener(task -> handleCartResult(task));
                    }
                } else {
                    // Nếu sản phẩm chưa tồn tại, thêm mới
                    cartRef.setValue(cartItem).addOnCompleteListener(task -> handleCartResult(task));
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

        // Xử lý chuyển đổi ID sản phẩm từ Long sang String cho tham chiếu Firebase
        String productId;
        Object idObj = item.getId();
        
        if (idObj instanceof Long) {
            productId = String.valueOf(((Long) idObj).longValue());
        } else if (idObj instanceof Integer) {
            productId = String.valueOf(((Integer) idObj).intValue());
        } else {
            productId = String.valueOf(idObj);
        }

        DatabaseReference favoriteRef = FirebaseDatabase.getInstance().getReference("Favorites")
                .child(currentUser.getUid())
                .child(productId); // Sử dụng productId đã được xử lý thay vì String.valueOf(item.getId())

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

    /**
     * Chuyển đổi trạng thái yêu thích của sản phẩm
     * Nếu đã yêu thích thì xóa khỏi danh sách yêu thích, ngược lại thì thêm vào
     */
    private void toggleFavorite() {
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để thêm vào yêu thích", Toast.LENGTH_SHORT).show();
            return;
        }

        // Xử lý chuyển đổi ID sản phẩm từ Long sang String
        String productId;
        Object idObj = item.getId();
        
        if (idObj instanceof Long) {
            productId = String.valueOf(((Long) idObj).longValue());
        } else if (idObj instanceof Integer) {
            productId = String.valueOf(((Integer) idObj).intValue());
        } else {
            productId = String.valueOf(idObj);
        }

        DatabaseReference favoriteRef = FirebaseDatabase.getInstance().getReference("Favorites")
                .child(currentUser.getUid())
                .child(productId); // Sử dụng productId đã được xử lý
        
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
            // Tạo đối tượng FavoriteItem với ID đã được xử lý
            FavoriteItem favoriteItem = new FavoriteItem(
                productId,  // Sử dụng productId đã được xử lý
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

    // Thêm các phương thức trợ giúp
    private String safeGetString(DataSnapshot snapshot, String key, String defaultValue) {
        Object value = snapshot.child(key).getValue();
        return value != null ? value.toString() : defaultValue;
    }

    
    private double safeGetDouble(DataSnapshot snapshot, String key, double defaultValue) {
        Object value = snapshot.child(key).getValue();
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    private int safeGetInt(DataSnapshot snapshot, String key, int defaultValue) {
        Object value = snapshot.child(key).getValue();
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
}
