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
import com.example.btlandroid.Model.User;
import com.example.btlandroid.R;
import com.example.btlandroid.databinding.ActivityChatBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private ActivityChatBinding binding;
    private MessageAdapter messageAdapter;
    private List<Message> messages;
    private String currentUserId, otherUserId;
    private DatabaseReference chatRef;

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

    private void loadUserInfo() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(otherUserId);
        
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    binding.userNameText.setText(user.getName());
                    if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                        Glide.with(ChatActivity.this)
                                .load(user.getProfileImage())
                                .placeholder(R.drawable.user_placeholder)
                                .into(binding.userAvatarImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMessages() {
        String chatRoomId = getChatRoomId(currentUserId, otherUserId);
        chatRef = FirebaseDatabase.getInstance().getReference("Chats").child(chatRoomId);
        
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
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

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

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

    private String getChatRoomId(String user1, String user2) {
        if (user1.compareTo(user2) > 0) {
            return user1 + "_" + user2;
        } else {
            return user2 + "_" + user1;
        }
    }
}
