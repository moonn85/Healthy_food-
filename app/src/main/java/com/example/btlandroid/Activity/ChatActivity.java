package com.example.btlandroid.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.btlandroid.Adapter.MessageAdapter;
import com.example.btlandroid.Model.Message;
import com.example.btlandroid.Domain.User;
import com.example.btlandroid.R;
import com.example.btlandroid.databinding.ActivityChatBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

// Lớp ChatActivity dùng để hiển thị giao diện trò chuyện giữa hai người dùng
// và xử lý các chức năng như gửi tin nhắn, tải ảnh lên, hiển thị thông tin người dùng.
public class ChatActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private ActivityChatBinding binding;
    private MessageAdapter messageAdapter;
    private List<com.example.btlandroid.Model.Message> messages;
    private String currentUserId, otherUserId;
    private DatabaseReference chatRef;
    private ValueEventListener userInfoListener;
    private ValueEventListener messagesListener;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Lấy ID người dùng hiện tại và người nhận
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        otherUserId = getIntent().getStringExtra("userId");

        // Khởi tạo RecyclerView
        messages = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messages);
        binding.messagesRecyclerView.setAdapter(messageAdapter);
        binding.messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Load thông tin người dùng
        loadUserInfo();

        // Load tin nhắn
        loadMessages();

        // Xử lý sự kiện gửi tin nhắn
        binding.sendButton.setOnClickListener(v -> sendMessage());

        // Xử lý nút quay lại
        binding.backButton.setOnClickListener(v -> finish());

        // Thêm xử lý nút chọn ảnh
        binding.attachImageButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Chọn ảnh"), PICK_IMAGE_REQUEST);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            uploadImage(imageUri);
        }
    }

    private void uploadImage(Uri imageUri) {
        if (imageUri != null) {
            StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("chat_images/" + System.currentTimeMillis() + ".jpg");

            storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        sendImageMessage(imageUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải ảnh lên: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
        }
    }

    private void sendImageMessage(String imageUrl) {
        String messageId = chatRef.push().getKey();
        Message message = new Message(
            messageId,
            currentUserId,
            otherUserId,
            imageUrl,
            System.currentTimeMillis(),
            true
        );

        if (messageId != null) {
            chatRef.child(messageId).setValue(message)
                .addOnFailureListener(e -> {
                    Toast.makeText(ChatActivity.this, 
                        "Lỗi gửi ảnh: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUserActiveStatus(true);
        // Đánh dấu tất cả tin nhắn là đã đọc
        if (chatRef != null) {
            chatRef.orderByChild("receiverId").equalTo(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            dataSnapshot.getRef().child("read").setValue(true);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateUserActiveStatus(false);
    }
    // Cập nhật trạng thái hoạt động của người dùng
    // Nếu người dùng đang online, lưu thời gian hiện tại vào trường lastActive
    private void updateUserActiveStatus(boolean isOnline) {
        if (currentUserId != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users")
                    .child(currentUserId);
            
            if (isOnline) {
                userRef.child("lastActive").setValue(ServerValue.TIMESTAMP);
            } else {
                userRef.child("lastActive").setValue(System.currentTimeMillis());
            }
        }
    }

    // Tải thông tin người dùng từ Firebase 
    // Hiển thị tên và ảnh đại diện của người dùng
    // Cập nhật trạng thái online/offline của người dùng
    private void loadUserInfo() {
        userRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(otherUserId);
        
        userInfoListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isDestroyed() && !isFinishing()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        binding.userNameText.setText(user.getName());
                        
                        // Hiển thị trạng thái online/offline
                        long lastActive = user.getLastActive();
                        long currentTime = System.currentTimeMillis();
                        long diffMinutes = (currentTime - lastActive) / (60 * 1000);
                        
                        String lastSeenText;
                        if (diffMinutes < 1) {
                            lastSeenText = "Online";
                            binding.lastActiveText.setTextColor(getResources().getColor(R.color.green));
                        } else if (diffMinutes < 60) {
                            lastSeenText = "Lần cuối " + diffMinutes + " phút trước";
                        } else if (diffMinutes < 24 * 60) {
                            lastSeenText = "Lần cuối " + (diffMinutes / 60) + " giờ trước"; 
                        } else {
                            lastSeenText = "Lần cuối " + (diffMinutes / (24 * 60)) + " ngày trước";
                        }
                        binding.lastActiveText.setText(lastSeenText);
                        
                        if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                            Glide.with(ChatActivity.this)
                                    .load(user.getProfileImage())
                                    .placeholder(R.drawable.user_placeholder)
                                    .into(binding.userAvatarImage);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        userRef.addValueEventListener(userInfoListener);
    }

    // Tải tin nhắn từ Firebase
    // Lắng nghe sự thay đổi dữ liệu trong chatRef và cập nhật danh sách tin nhắn
    private void loadMessages() {
        String chatRoomId = getChatRoomId(currentUserId, otherUserId);
        chatRef = FirebaseDatabase.getInstance().getReference("Chats").child(chatRoomId);
        
        messagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isDestroyed() && !isFinishing()) {
                    messages.clear();
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        Message message = dataSnapshot.getValue(Message.class);
                        if (message != null) {
                            messages.add(message);
                        }
                    }
                    messageAdapter.notifyDataSetChanged();
                    binding.messagesRecyclerView.scrollToPosition(messages.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        chatRef.addValueEventListener(messagesListener);
    }

    // Gửi tin nhắn từ EditText
    // Nếu tin nhắn không rỗng, tạo một Message mới và lưu vào Firebase
    private void sendMessage() {
        String content = binding.messageEditText.getText().toString().trim();
        if (TextUtils.isEmpty(content)) return;

        String messageId = chatRef.push().getKey();
        Message message = new Message(
                messageId,
                currentUserId,
                otherUserId,
                content,
                System.currentTimeMillis()
        );

        if (messageId != null) {
            chatRef.child(messageId).setValue(message)
                    .addOnSuccessListener(aVoid -> {
                        binding.messageEditText.setText("");
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ChatActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // Tạo ID phòng chat dựa trên ID của hai người dùng
    // Đảm bảo ID phòng chat là duy nhất và không bị trùng lặp
    private String getChatRoomId(String user1, String user2) {
        if (user1.compareTo(user2) > 0) {
            return user1 + "_" + user2;
        } else {
            return user2 + "_" + user1;
        }
    }

    // Xóa listener khi Activity bị hủy để tránh rò rỉ bộ nhớ
    // Nếu có listener đang hoạt động, xóa nó để không còn lắng nghe sự thay đổi dữ liệu nữa
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userInfoListener != null && userRef != null) {
            userRef.removeEventListener(userInfoListener);
        }
        if (messagesListener != null && chatRef != null) {
            chatRef.removeEventListener(messagesListener);
        }
    }
}
