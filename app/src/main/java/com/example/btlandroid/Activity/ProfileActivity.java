package com.example.btlandroid.Activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.bumptech.glide.Glide;
import com.example.btlandroid.R;
import com.example.btlandroid.Utils.BottomNavigationUtils; // Add this import
import com.example.btlandroid.databinding.ActivityProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.btlandroid.Utils.ToastUtils;
import com.squareup.picasso.Picasso;

// để hiển thị thông tin người dùng và cho phép họ chỉnh sửa hồ sơ của mình
// sử dụng Firebase để lưu trữ thông tin người dùng
public class ProfileActivity extends AppCompatActivity {
    private ActivityProfileBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseUser currentUser;

    // Thêm ActivityResultLauncher để nhận kết quả từ EditProfileActivity
    private final ActivityResultLauncher<Intent> editProfileLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                // Cập nhật UI với thông tin mới
                String newName = result.getData().getStringExtra("name");
                String newImageUrl = result.getData().getStringExtra("imageUrl");
                
                binding.userNameTextView.setText(newName);
                if (newImageUrl != null && !newImageUrl.isEmpty()) {
                    Picasso.get().load(newImageUrl).into(binding.profileImageView);
                }
            }
        }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Kiểm tra xem người dùng có phải admin không
        checkAdminStatus();
        
        // Hiển thị thông tin người dùng
        loadUserProfile();

        // Thiết lập các sự kiện click cho các button
        setupClickListeners();
    }

    private void checkAdminStatus() {
        if (currentUser != null) {
            DatabaseReference adminRef = FirebaseDatabase.getInstance().getReference("Admins")
                    .child(currentUser.getUid());

            adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Người dùng là admin, hiển thị nút quay lại trang admin
                        binding.adminPanelButton.setVisibility(View.VISIBLE);
                        binding.adminPanelButton.setOnClickListener(v -> {
                            startActivity(new Intent(ProfileActivity.this, AdminActivity.class));
                        });
                    } else {
                        // Không phải admin, ẩn nút
                        binding.adminPanelButton.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Xử lý lỗi nếu có
                    binding.adminPanelButton.setVisibility(View.GONE);
                }
            });
        }
    }

    private void loadUserProfile() {
        // Hiển thị thông tin người dùng từ Firebase
        if (currentUser != null) {
            // Hiển thị email
            binding.userEmailTextView.setText(currentUser.getEmail());

            // Lấy thông tin profile từ database
            mDatabase = FirebaseDatabase.getInstance().getReference("Users")
                    .child(currentUser.getUid());
            
            mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Hiển thị tên người dùng
                        String name = snapshot.child("name").getValue(String.class);
                        if (name != null && !name.isEmpty()) {
                            binding.userNameTextView.setText(name);
                        } else {
                            binding.userNameTextView.setText("Người dùng");
                        }
                        
                        // Hiển thị ảnh đại diện nếu có
                        String profileImage = snapshot.child("profileImage").getValue(String.class);
                        if (profileImage != null && !profileImage.isEmpty()) {
                            Picasso.get().load(profileImage).into(binding.profileImageView);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Xử lý lỗi
                }
            });
        }
    }

    private void setupClickListeners() {
        // Xử lý sự kiện nút đăng xuất
        binding.logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
        });

        // Cập nhật xử lý cho nút chỉnh sửa hồ sơ
        binding.editProfileButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            intent.putExtra("name", binding.userNameTextView.getText().toString());
            // Truyền URL ảnh hiện tại nếu có
            if (currentUser != null) {
                DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(currentUser.getUid());
                userRef.child("profileImage").get().addOnSuccessListener(snapshot -> {
                    String imageUrl = snapshot.getValue(String.class);
                    if (imageUrl != null) {
                        intent.putExtra("imageUrl", imageUrl);
                    }
                    editProfileLauncher.launch(intent);
                });
            }
        });

        // Xử lý sự kiện nút đơn hàng của tôi
        binding.myOrdersButton.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, MyOrdersActivity.class));
        });

        // Sửa lại phần xử lý chat với người bán
        binding.chatButton.setOnClickListener(v -> {
            // Đầu tiên tìm trong bảng Users những người có isAdmin = true
            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
            usersRef.orderByChild("isAdmin").equalTo(true)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Tìm thấy admin trong bảng Users
                            for (DataSnapshot adminSnapshot : snapshot.getChildren()) {
                                String adminId = adminSnapshot.getKey();
                                Intent intent = new Intent(ProfileActivity.this, ChatActivity.class);
                                intent.putExtra("userId", adminId);
                                startActivity(intent);
                                return;
                            }
                        } else {
                            // Nếu không tìm thấy trong Users, kiểm tra trong bảng Admins
                            DatabaseReference adminsRef = FirebaseDatabase.getInstance().getReference("Admins");
                            adminsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (snapshot.exists()) {
                                        for (DataSnapshot adminSnapshot : snapshot.getChildren()) {
                                            String adminId = adminSnapshot.getKey();
                                            Intent intent = new Intent(ProfileActivity.this, ChatActivity.class);
                                            intent.putExtra("userId", adminId);
                                            startActivity(intent);
                                            return;
                                        }
                                    }
                                    // Nếu vẫn không tìm thấy
                                    Toast.makeText(ProfileActivity.this, 
                                        "Không tìm thấy người bán, vui lòng thử lại sau", 
                                        Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(ProfileActivity.this, 
                                        "Lỗi: " + error.getMessage(), 
                                        Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ProfileActivity.this, 
                            "Lỗi: " + error.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    }
                });
        });

        // Thiết lập các xử lý cho bottom navigation
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        // Bottom navigation
        binding.bottomNavigation.homeNav.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, MainActivity.class));
            finish();
        });

        binding.bottomNavigation.searchNav.setOnClickListener(v -> {
            // Xử lý chuyển đến màn hình tìm kiếm
        });

        binding.bottomNavigation.favoriteNav.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, FavoriteActivity.class));
            finish();
        });

        binding.bottomNavigation.cartNav.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, CartActivity.class));
            finish();
        });

    }
}
