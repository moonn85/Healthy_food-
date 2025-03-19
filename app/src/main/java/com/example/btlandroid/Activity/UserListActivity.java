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
import com.example.btlandroid.Model.Message; // Add this import
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
            chatRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // Map để lưu timestamp của tin nhắn cuối cùng cho mỗi user
                    Map<String, UserWithLastMessage> userMessages = new HashMap<>();

                    // Duyệt qua tất cả chat rooms
                    for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                        String roomId = roomSnapshot.getKey();
                        if (roomId != null && roomId.contains(currentUserId)) {
                            // Lấy ID của user khác từ room ID
                            String otherUserId = roomId.replace(currentUserId + "_", "")
                                    .replace("_" + currentUserId, "");

                            // Lấy tin nhắn cuối cùng của room này
                            Message lastMessage = null;
                            long lastTimestamp = 0;
                            for (DataSnapshot messageSnapshot : roomSnapshot.getChildren()) {
                                Message message = messageSnapshot.getValue(Message.class);
                                if (message != null && message.getTimestamp() > lastTimestamp) {
                                    lastMessage = message;
                                    lastTimestamp = message.getTimestamp();
                                }
                            }

                            if (lastMessage != null) {
                                userMessages.put(otherUserId, new UserWithLastMessage(otherUserId, lastTimestamp));
                            }
                        }
                    }

                    // Lấy thông tin user và sắp xếp theo thời gian tin nhắn cuối
                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            List<UserWithLastMessage> sortedUsers = new ArrayList<>();
                            
                            for (Map.Entry<String, UserWithLastMessage> entry : userMessages.entrySet()) {
                                DataSnapshot userSnapshot = snapshot.child(entry.getKey());
                                User user = userSnapshot.getValue(User.class);
                                if (user != null) {
                                    entry.getValue().setUser(user);
                                    sortedUsers.add(entry.getValue());
                                }
                            }

                            // Sắp xếp theo timestamp giảm dần
                            Collections.sort(sortedUsers, (a, b) -> 
                                Long.compare(b.getLastMessageTime(), a.getLastMessageTime()));

                            // Cập nhật danh sách user
                            userList.clear();
                            for (UserWithLastMessage uwm : sortedUsers) {
                                userList.add(uwm.getUser());
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
