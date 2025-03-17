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

public class UserListActivity extends AppCompatActivity implements UserAdapter.OnUserClickListener {
    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private List<User> userList;
    private boolean isAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        recyclerView = findViewById(R.id.userRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        userList = new ArrayList<>();
        userAdapter = new UserAdapter(this, userList, this);
        recyclerView.setAdapter(userAdapter);

        // Kiểm tra xem người dùng hiện tại có phải là admin không
        checkAdminStatus();

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
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
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users");
        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("Chats");

        if (isAdmin) {
            // Nếu là admin, hiển thị tất cả người dùng có tin nhắn, sắp xếp theo tin nhắn mới nhất
            chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot chatSnapshot) {
                    List<String> userIdsWithMessages = new ArrayList<>();
                    long latestTimestamp = 0;
                    
                    // Lọc ra các user có chat với admin
                    for (DataSnapshot chat : chatSnapshot.getChildren()) {
                        String chatId = chat.getKey();
                        if (chatId != null && chatId.contains(currentUserId)) {
                            String otherUserId = chatId.replace(currentUserId + "_", "")
                                               .replace("_" + currentUserId, "");
                            if (!otherUserId.equals(currentUserId)) {
                                userIdsWithMessages.add(otherUserId);
                            }
                        }
                    }

                    // Lấy thông tin user từ danh sách ID
                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            userList.clear();
                            for (String userId : userIdsWithMessages) {
                                DataSnapshot userSnapshot = snapshot.child(userId);
                                User user = userSnapshot.getValue(User.class);
                                if (user != null && !user.isAdmin()) {
                                    userList.add(user);
                                }
                            }
                            if (userList.isEmpty()) {
                                Toast.makeText(UserListActivity.this, 
                                    "Chưa có cuộc trò chuyện nào", 
                                    Toast.LENGTH_SHORT).show();
                            }
                            userAdapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(UserListActivity.this, 
                                "Lỗi: " + error.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(UserListActivity.this, 
                        "Lỗi: " + error.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Nếu là user thường, chỉ tìm admin
            userRef.orderByChild("isAdmin").equalTo(true)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        userList.clear();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            User user = dataSnapshot.getValue(User.class);
                            if (user != null) {
                                userList.add(user);
                            }
                        }
                        if (userList.isEmpty()) {
                            Toast.makeText(UserListActivity.this, 
                                "Không tìm thấy người bán", 
                                Toast.LENGTH_SHORT).show();
                        }
                        userAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(UserListActivity.this, 
                            "Lỗi: " + error.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    }
                });
        }
    }

    @Override
    public void onUserClick(User user) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("userId", user.getId());
        startActivity(intent);
    }
}
