package com.example.btlandroid.Activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.btlandroid.R;
import com.example.btlandroid.databinding.ActivityEditProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {
    private ActivityEditProfileBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private StorageReference storageRef;
    private Uri imageUri;
    private String currentImageUrl;
    
    // Launcher để chọn ảnh
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();
                if (data != null && data.getData() != null) {
                    imageUri = data.getData();
                    Glide.with(this).load(imageUri).into(binding.profileImageView);
                }
            }
        }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid());
        storageRef = FirebaseStorage.getInstance().getReference("profile_images");

        // Lấy dữ liệu từ intent
        String name = getIntent().getStringExtra("name");
        currentImageUrl = getIntent().getStringExtra("imageUrl");

        // Hiển thị dữ liệu hiện tại
        binding.nameEditText.setText(name);
        if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
            Glide.with(this).load(currentImageUrl).into(binding.profileImageView);
        }

        // Xử lý sự kiện click
        binding.changeImageButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            pickImage.launch(intent);
        });

        binding.saveButton.setOnClickListener(v -> saveChanges());
    }

    private void saveChanges() {
        String newName = binding.nameEditText.getText().toString().trim();
        if (newName.isEmpty()) {
            binding.nameEditText.setError("Vui lòng nhập tên");
            return;
        }

        // Hiển thị loading
        binding.saveButton.setEnabled(false);
        binding.saveButton.setText("Đang lưu...");

        if (imageUri != null) {
            // Upload ảnh mới
            String fileName = mAuth.getCurrentUser().getUid();
            StorageReference imageRef = storageRef.child(fileName);
            
            imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        updateProfile(newName, uri.toString());
                    });
                })
                .addOnFailureListener(e -> {
                    binding.saveButton.setEnabled(true);
                    binding.saveButton.setText("Lưu thay đổi");
                    Toast.makeText(this, "Lỗi khi tải ảnh lên", Toast.LENGTH_SHORT).show();
                });
        } else {
            // Cập nhật chỉ tên
            updateProfile(newName, currentImageUrl);
        }
    }

    private void updateProfile(String name, String imageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("profileImage", imageUrl);

        userRef.updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show();
                Intent resultIntent = new Intent();
                resultIntent.putExtra("name", name);
                resultIntent.putExtra("imageUrl", imageUrl);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            })
            .addOnFailureListener(e -> {
                binding.saveButton.setEnabled(true);
                binding.saveButton.setText("Lưu thay đổi");
                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
}
