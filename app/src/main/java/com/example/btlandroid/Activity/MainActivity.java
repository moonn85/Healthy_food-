package com.example.btlandroid.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.btlandroid.Adapter.PhanloaiAdapter;
import com.example.btlandroid.Domain.LocationDomain;
import com.example.btlandroid.Domain.PhanloaiDomain;
import com.example.btlandroid.Domain.ItemDomain;
import com.example.btlandroid.Adapter.BestdealAdapter;
import com.example.btlandroid.R;
import com.example.btlandroid.Utils.BottomNavigationUtils;
import com.example.btlandroid.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseUser currentUser;
    private FirebaseDatabase database;
    private PhanloaiAdapter phanloaiAdapter;
    private final ArrayList<PhanloaiDomain> phanloaiList = new ArrayList<>();
    private final ArrayList<ItemDomain> bestDealList = new ArrayList<>();
    private BestdealAdapter bestDealAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Kiểm tra xem binding có null không
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        if (binding == null) {
            Toast.makeText(this, "Lỗi: binding null!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(binding.getRoot());

        // Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        
        // Kiểm tra nếu người dùng chưa đăng nhập, quay lại màn hình Start
        if (currentUser == null) {
            startActivity(new Intent(MainActivity.this, StartActivity.class));
            finish();
            return;
        }

        // Khởi tạo Firebase Database reference
        mDatabase = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
        
        // Lấy dữ liệu người dùng từ Firebase
        getUserData();

        // Khởi tạo Firebase
        database = FirebaseDatabase.getInstance();
        if (database == null) {
            Toast.makeText(this, "Lỗi: Firebase chưa được khởi tạo!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Khởi tạo RecyclerViews với adapter trống ngay từ đầu
        initRecyclerViews();
        
        // Tải dữ liệu
        initLocation();
        initPhanloaiList();
        initBestDealList();

        // Thay thế binding.imageViewProfile và binding.textViewProfile với findViewById
        ImageView profileImage = findViewById(R.id.imageViewProfile);
        TextView profileText = findViewById(R.id.textViewProfile);
        
        profileImage.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        });

        profileText.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        });

        // Thiết lập sự kiện cho thanh điều hướng dưới cùng
        BottomNavigationUtils.setupBottomNavigation(this, R.id.homeNav);
    }

    private void initRecyclerViews() {
        // Khởi tạo adapter cho RecyclerView phân loại
        phanloaiAdapter = new PhanloaiAdapter(phanloaiList);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerView.setAdapter(phanloaiAdapter);

        // Khởi tạo adapter cho RecyclerView best deal
        bestDealAdapter = new BestdealAdapter(bestDealList);
        binding.bestdealview.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
        binding.bestdealview.setAdapter(bestDealAdapter);
    }

    private void initPhanloaiList() {
        // Thay "Phanloai" bằng "Category" - tên node thực tế trong Firebase
        DatabaseReference myRef = database.getReference("Category");
        binding.progressBar2.setVisibility(View.VISIBLE);

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                phanloaiList.clear();
                if (snapshot.exists()) {
                    for(DataSnapshot issue : snapshot.getChildren()) {
                        PhanloaiDomain item = issue.getValue(PhanloaiDomain.class);
                        if (item != null) {
                            phanloaiList.add(item);
                        }
                    }
                    phanloaiAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(MainActivity.this, "Không có dữ liệu phân loại", Toast.LENGTH_SHORT).show();
                }
                binding.progressBar2.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Lỗi tải dữ liệu phân loại: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                binding.progressBar2.setVisibility(View.GONE);
            }
        });
    }

    private void initBestDealList() {
        // Có thể sử dụng node "Items" và thêm query để lọc các mục "best deal"
        // hoặc bạn cần tạo node "BestDeal" trên Firebase
        DatabaseReference myRef = database.getReference("Items");
        binding.progressBarbestdeal.setVisibility(View.VISIBLE);

        // Có thể giới hạn số lượng items hiển thị làm "Best Deal"
        myRef.limitToFirst(5).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bestDealList.clear();
                if (snapshot.exists()) {
                    for(DataSnapshot issue : snapshot.getChildren()) {
                        ItemDomain item = issue.getValue(ItemDomain.class);
                        if (item != null) {
                            bestDealList.add(item);
                        }
                    }
                    bestDealAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(MainActivity.this, "Không có dữ liệu best deal", Toast.LENGTH_SHORT).show();
                }
                binding.progressBarbestdeal.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Lỗi tải dữ liệu best deal: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                binding.progressBarbestdeal.setVisibility(View.GONE);
            }
        });
    }

    private void initLocation() {
        DatabaseReference myRef = database.getReference("Location");
        ArrayList<LocationDomain> list = new ArrayList<>();

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot issue : snapshot.getChildren()) {
                        LocationDomain location = issue.getValue(LocationDomain.class);
                        if (location != null) {
                            list.add(location);
                        }
                    }
                    if (!list.isEmpty()) {
                        ArrayAdapter<LocationDomain> adapter = new ArrayAdapter<>(MainActivity.this, R.layout.sp_item, list);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        binding.locationSp.setAdapter(adapter);
                    } else {
                        Toast.makeText(MainActivity.this, "Không có dữ liệu Location!", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Lỗi Firebase: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void getUserData() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String profileImage = snapshot.child("profileImage").getValue(String.class);
                    
                    // Hiển thị thông tin người dùng nếu cần
                    // Ví dụ: binding.userNameTextView.setText(name);
                    
                    // Nếu có ảnh đại diện, hiển thị bằng Glide
                    if (profileImage != null && !profileImage.isEmpty()) {
                        // Giả sử bạn có ImageView để hiển thị ảnh đại diện
                        // Glide.with(MainActivity.this).load(profileImage).into(binding.profileImageView);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Không thể tải dữ liệu người dùng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_logout) {
            // Đăng xuất người dùng
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(MainActivity.this, StartActivity.class));
            finish();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
}
