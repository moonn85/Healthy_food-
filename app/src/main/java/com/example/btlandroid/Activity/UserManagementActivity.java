package com.example.btlandroid.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btlandroid.Adapter.UserManagementAdapter;
import com.example.btlandroid.Model.User;
import com.example.btlandroid.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class UserManagementActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private UserManagementAdapter adapter;
    private List<User> userList;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        // Khởi tạo Firebase
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        
        // Ánh xạ views
        recyclerView = findViewById(R.id.usersRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Khởi tạo list và adapter
        userList = new ArrayList<>();
        adapter = new UserManagementAdapter(userList);
        recyclerView.setAdapter(adapter);

        // Nút quay lại
        findViewById(R.id.backBtn).setOnClickListener(v -> finish());

        // Tải danh sách người dùng
        loadUsers();
    }

    private void loadUsers() {
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        findViewById(R.id.emptyTextView).setVisibility(View.GONE);
        
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String userId = dataSnapshot.getKey();
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null) {
                        user.setId(userId); // Đảm bảo lưu ID người dùng
                        userList.add(user);
                    }
                }
                
                findViewById(R.id.progressBar).setVisibility(View.GONE);
                
                if (userList.isEmpty()) {
                    findViewById(R.id.emptyTextView).setVisibility(View.VISIBLE);
                }
                
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                findViewById(R.id.progressBar).setVisibility(View.GONE);
                Toast.makeText(UserManagementActivity.this, 
                    "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (adapter != null) {
            adapter.handleImageResult(requestCode, resultCode, data);
        }
    }
}
