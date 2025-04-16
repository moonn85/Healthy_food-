package com.example.btlandroid.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btlandroid.Adapter.UserAdapter;
import com.example.btlandroid.Model.Message;
import com.example.btlandroid.Model.User;
import com.example.btlandroid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Comparator;

// để hiển thị danh sách người dùng trong ứng dụng
// sử dụng Firebase Realtime Database để lưu trữ thông tin người dùng
public class UserListActivity extends AppCompatActivity implements UserAdapter.OnUserClickListener {
    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private List<User> userList;
    private boolean isAdmin = false;
    private String currentUserId;
    private DatabaseReference userRef;
    private View progressBar; // Add a reference to progressBar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("Users");

        recyclerView = findViewById(R.id.userRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Find progressBar view
        progressBar = findViewById(R.id.progressBar);
        
        userList = new ArrayList<>();
        userAdapter = new UserAdapter(this, userList, this);
        recyclerView.setAdapter(userAdapter);

        // Kiểm tra xem người dùng hiện tại có phải là admin không
        checkAdminStatus();

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        
        // Set current user as online
        updateUserOnlineStatus(true);
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
            DatabaseReference userStatusRef = userRef.child(currentUserId);
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

    private void checkAdminStatus() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference adminRef = FirebaseDatabase.getInstance().getReference("Admins")
                .child(currentUserId);

        adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isAdmin = snapshot.exists();
                // Load danh sách người dùng dựa vào vai trò
                loadUsers();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserListActivity.this, "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUsers() {
        // Safely show progress bar if it exists
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        if (isAdmin) {
            // ...existing admin code...
            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    userList.clear();
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        User user = dataSnapshot.getValue(User.class);
                        if (user != null && !user.getId().equals(currentUserId)) {
                            // Ensure user ID is set
                            String userId = dataSnapshot.getKey();
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
                    
                    userAdapter.notifyDataSetChanged();
                    // Safely hide progress bar if it exists
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Safely hide progress bar if it exists
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    Toast.makeText(UserListActivity.this, 
                        "Lỗi: " + error.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Nếu là user thường, chỉ tìm admin
            userRef.orderByChild("isAdmin").equalTo(true)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        userList.clear();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            User user = dataSnapshot.getValue(User.class);
                            if (user != null) {
                                String userId = dataSnapshot.getKey();
                                user.setId(userId);
                                userList.add(user);
                            }
                        }
                        
                        // Sort users: online first, then by last active time
                        Collections.sort(userList, (u1, u2) -> {
                            if (u1.isOnline() && !u2.isOnline()) {
                                return -1;
                            } else if (!u1.isOnline() && u2.isOnline()) {
                                return 1;
                            } else {
                                return Long.compare(u2.getLastActive(), u1.getLastActive());
                            }
                        });
                        
                        userAdapter.notifyDataSetChanged();
                        // Safely hide progress bar if it exists
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        
                        if (userList.isEmpty()) {
                            Toast.makeText(UserListActivity.this, 
                                "Không tìm thấy người bán", 
                                Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Safely hide progress bar if it exists
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        Toast.makeText(UserListActivity.this, 
                            "Lỗi: " + error.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    }
                });
        }
    }

    // Class phụ trợ để sắp xếp user theo thời gian tin nhắn cuối
    private static class UserWithLastMessage {
        private String userId;
        private User user;
        private long lastMessageTime;

        public UserWithLastMessage(String userId, long lastMessageTime) {
            this.userId = userId;
            this.lastMessageTime = lastMessageTime;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public User getUser() {
            return user;
        }

        public long getLastMessageTime() {
            return lastMessageTime;
        }
    }

    @Override
    public void onUserClick(User user) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("userId", user.getId());
        startActivity(intent);
    }
}
