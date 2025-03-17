package com.example.btlandroid.Activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.btlandroid.Adapter.BestdealAdapter;
import com.example.btlandroid.Domain.ItemDomain;
import com.example.btlandroid.databinding.ActivityCategoryItemsBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class CategoryItemsActivity extends AppCompatActivity {
    
    private ActivityCategoryItemsBinding binding;
    private BestdealAdapter itemAdapter;
    private ArrayList<ItemDomain> itemList = new ArrayList<>();
    private int categoryId = -1;
    private String categoryName = "";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCategoryItemsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Nhận dữ liệu từ Intent
        if (getIntent() != null) {
            categoryId = getIntent().getIntExtra("categoryId", -1);
            categoryName = getIntent().getStringExtra("categoryName");
            
            // Hiển thị tên phân loại trên toolbar
            binding.categoryTitleTxt.setText(categoryName);
        }
        
        // Cài đặt RecyclerView
        setupRecyclerView();
        
        // Load dữ liệu sản phẩm theo categoryId
        loadCategoryItems();
        
        // Thiết lập sự kiện cho nút Back
        binding.backBtn.setOnClickListener(v -> finish());
    }
    
    private void setupRecyclerView() {
        itemAdapter = new BestdealAdapter(itemList);
        binding.itemsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        binding.itemsRecyclerView.setAdapter(itemAdapter);
    }
    
    private void loadCategoryItems() {
        if (categoryId == -1) {
            Toast.makeText(this, "Không tìm thấy thông tin phân loại", Toast.LENGTH_SHORT).show();
            return;
        }
        
        binding.progressBar.setVisibility(View.VISIBLE);
        
        // Kết nối với Firebase và truy vấn dữ liệu
        DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("Items");
        
        // Sửa tên trường từ "categoryId" thành "CategoryId" để phù hợp với dữ liệu Firebase
        Query query = itemsRef.orderByChild("CategoryId").equalTo(categoryId);
        
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                itemList.clear();
                
                // Thêm log để kiểm tra dữ liệu
                Toast.makeText(CategoryItemsActivity.this, 
                        "Số sản phẩm tìm được: " + snapshot.getChildrenCount(), 
                        Toast.LENGTH_SHORT).show();
                
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    ItemDomain item = itemSnapshot.getValue(ItemDomain.class);
                    if (item != null) {
                        itemList.add(item);
                    }
                }
                
                // Hiển thị thông báo nếu không có sản phẩm
                if (itemList.isEmpty()) {
                    binding.emptyTextView.setVisibility(View.VISIBLE);
                } else {
                    binding.emptyTextView.setVisibility(View.GONE);
                }
                
                // Cập nhật adapter
                itemAdapter.notifyDataSetChanged();
                binding.progressBar.setVisibility(View.GONE);
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CategoryItemsActivity.this, 
                        "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                binding.progressBar.setVisibility(View.GONE);
            }
        });
    }
}
