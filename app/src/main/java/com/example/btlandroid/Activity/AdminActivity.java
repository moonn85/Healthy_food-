package com.example.btlandroid.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.btlandroid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// để quản lý các chức năng của admin trong ứng dụng
// bao gồm quản lý sản phẩm, đơn hàng, người dùng và thống kê thu nhập
public class AdminActivity extends AppCompatActivity {

    private CardView manageProductsButton, manageOrdersButton, manageUsersButton, incomeStatisticsButton;
    private ImageView imageViewHome, imageViewProfile, imageViewLogout;
    private TextView adminEmailTextView;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Ánh xạ các view
        manageProductsButton = findViewById(R.id.manageProductsButton);
        manageOrdersButton = findViewById(R.id.manageOrdersButton);
        manageUsersButton = findViewById(R.id.manageUsersButton);
        incomeStatisticsButton = findViewById(R.id.incomeStatisticsButton);
        
        imageViewHome = findViewById(R.id.imageViewHome);
        imageViewProfile = findViewById(R.id.imageViewProfile);
        imageViewLogout = findViewById(R.id.imageViewLogout);
        
        adminEmailTextView = findViewById(R.id.adminEmailTextView);

        // Hiển thị email của admin
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            adminEmailTextView.setText("Admin: " + currentUser.getEmail());
        }

        // Xử lý sự kiện click cho các nút quản lý
        manageProductsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Chuyển đến trang quản lý sản phẩm
                Intent intent = new Intent(AdminActivity.this, ProductManagementActivity.class);
                startActivity(intent);
            }
        });

        manageOrdersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Chuyển đến trang quản lý đơn hàng
                Intent intent = new Intent(AdminActivity.this, OrderManagementActivity.class);
                startActivity(intent);
            }
        });

        manageUsersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Chuyển đến trang quản lý người dùng
                Intent intent = new Intent(AdminActivity.this, UserManagementActivity.class);
                startActivity(intent);
            }
        });

        // Thêm xử lý sự kiện cho nút thống kê thu nhập
        incomeStatisticsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Chuyển đến trang thống kê thu nhập
                Intent intent = new Intent(AdminActivity.this, IncomeStatisticsActivity.class);
                startActivity(intent);
            }
        });

        // Xử lý sự kiện click cho bottom navigation
        imageViewHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        imageViewProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });

        imageViewLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Đăng xuất và chuyển về màn hình đăng nhập
                mAuth.signOut();
                Intent intent = new Intent(AdminActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        findViewById(R.id.imageViewChat).setOnClickListener(v -> {
            startActivity(new Intent(AdminActivity.this, UserListActivity.class));
        });
    }
}
