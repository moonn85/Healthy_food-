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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// để quản lý người dùng trong ứng dụng
// sử dụng Firebase Realtime Database để lưu trữ thông tin người dùng
public class UserManagementActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private UserManagementAdapter adapter;
    private List<User> userList;
    private DatabaseReference usersRef;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        // Khởi tạo Firebase
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Ánh xạ views
        recyclerView = findViewById(R.id.usersRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Khởi tạo list và adapter
        userList = new ArrayList<>();
        adapter = new UserManagementAdapter(userList);
        recyclerView.setAdapter(adapter);

        // Nút quay lại
        findViewById(R.id.backBtn).setOnClickListener(v -> finish());

        // Update admin's online status
        updateUserOnlineStatus(true);
        
        // Tải danh sách người dùng
        loadUsers();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateUserOnlineStatus(true);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        updateUserOnlineStatus(false);
    }
    
    private void updateUserOnlineStatus(boolean isOnline) {
        if (currentUserId != null) {
            DatabaseReference userStatusRef = usersRef.child(currentUserId);
            if (isOnline) {
                userStatusRef.child("online").setValue(true);
            } else {
                Map<String, Object> updates = new HashMap<>();
                updates.put("online", false);
                updates.put("lastActive", System.currentTimeMillis());
                userStatusRef.updateChildren(updates);
            }
        }
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
                        // Ensure user ID is set
                        user.setId(userId);
                        userList.add(user);
                    }
                }
                
                // Sort users: online first, then by last active time
                Collections.sort(userList, new Comparator<User>() {
                    @Override
                    public int compare(User u1, User u2) {
                        if (u1.isOnline() && !u2.isOnline()) {
                            return -1;
                        } else if (!u1.isOnline() && u2.isOnline()) {
                            return 1;
                        } else {
                            return Long.compare(u2.getLastActive(), u1.getLastActive());
                        }
                    }
                });
                
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
