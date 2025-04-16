package com.example.btlandroid.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btlandroid.Adapter.ProductAdminAdapter;
import com.example.btlandroid.Domain.ProductDomain;
import com.example.btlandroid.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

// để quản lý sản phẩm trong ứng dụng
// sử dụng Firebase Realtime Database để lưu trữ thông tin sản phẩm
public class ProductManagementActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProductAdminAdapter adapter;
    private ArrayList<ProductDomain> productList;
    private DatabaseReference databaseRef; // Thay đổi từ Firestore sang Realtime Database
    private ProgressBar progressBar;
    private TextView emptyTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_management);

        // Khởi tạo Firebase Realtime Database thay vì Firestore
        databaseRef = FirebaseDatabase.getInstance().getReference("Items");
        
        // Ánh xạ các view
        recyclerView = findViewById(R.id.productsRecyclerView);
        FloatingActionButton addProductBtn = findViewById(R.id.addProductBtn);
        ImageButton backBtn = findViewById(R.id.backBtn);
        progressBar = findViewById(R.id.progressBar);
        emptyTextView = findViewById(R.id.emptyTextView);

        // Thiết lập RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        productList = new ArrayList<>();
        adapter = new ProductAdminAdapter(productList, this);
        recyclerView.setAdapter(adapter);

        // Xử lý sự kiện khi nhấn nút trở về
        backBtn.setOnClickListener(v -> finish());

        // Xử lý sự kiện khi nhấn nút thêm sản phẩm
        addProductBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ProductManagementActivity.this, ProductEditActivity.class);
            intent.putExtra("isNewProduct", true);
            startActivity(intent);
        });

        // Tải dữ liệu sản phẩm
        loadProducts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Tải lại dữ liệu khi quay lại màn hình
        loadProducts();
    }

    private void loadProducts() {
        progressBar.setVisibility(View.VISIBLE);
        emptyTextView.setVisibility(View.GONE);
        
        // Lấy dữ liệu từ Realtime Database thay vì Firestore
        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                productList.clear();
                
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String id = dataSnapshot.getKey();
                    String title = dataSnapshot.child("Title").getValue(String.class);
                    String pic = dataSnapshot.child("ImagePath").getValue(String.class);
                    String description = dataSnapshot.child("Description").getValue(String.class);
                    Double price = dataSnapshot.child("Price").getValue(Double.class);
                    Double score = dataSnapshot.child("Star").getValue(Double.class);
                    Integer categoryId = dataSnapshot.child("CategoryId").getValue(Integer.class);
                    Integer locationId = dataSnapshot.child("LocationId").getValue(Integer.class);
                    
                    String categoryType = getCategoryName(categoryId);
                    String location = getLocationName(locationId);
                    
                    ProductDomain product = new ProductDomain(id, title, pic, description, 
                            price, 0, categoryType, score, 0, location);
                    product.setStock(10); // Mặc định số lượng tồn kho
                    productList.add(product);
                }
                
                // Cập nhật adapter
                adapter.notifyDataSetChanged();
                
                // Hiển thị thông báo nếu không có sản phẩm
                if (productList.isEmpty()) {
                    emptyTextView.setVisibility(View.VISIBLE);
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ProductManagementActivity.this, 
                        "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    // Phương thức chuyển đổi từ CategoryId sang tên danh mục
    private String getCategoryName(Integer categoryId) {
        // Mặc định trả về "Khác" nếu không xác định
        if (categoryId == null) return "Khác";
        
        switch(categoryId) {
            case 0: return "Rau";
            case 1: return "Hoa quả";
            case 2: return "Sữa";
            case 3: return "Đồ uống";
            case 4: return "Hạt";
            default: return "Khác";
        }
    }

    // Phương thức chuyển đổi từ LocationId sang tên vị trí
    private String getLocationName(Integer locationId) {
        // Mặc định trả về "Không xác định" nếu null
        if (locationId == null) return "Không xác định";
        
        switch(locationId) {
            case 0: return "Nước ngoài";
            case 1: return "Việt Nam";
            default: return "Không xác định";
        }
    }
}
