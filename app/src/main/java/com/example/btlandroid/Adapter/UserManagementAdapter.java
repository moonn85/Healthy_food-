package com.example.btlandroid.Adapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.btlandroid.Model.User;
import com.example.btlandroid.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

// Lớp UserManagementAdapter để quản lý danh sách người dùng trong ứng dụng Android
public class UserManagementAdapter extends RecyclerView.Adapter<UserManagementAdapter.ViewHolder> {

    private List<User> userList;
    private Context context;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;
    private CircleImageView currentImageView;

    public UserManagementAdapter(List<User> userList) {
        this.userList = userList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_management, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = userList.get(position);

        // Hiển thị thông tin người dùng
        holder.userNameTextView.setText(user.getName());
        holder.userEmailTextView.setText(user.getEmail());

        // Tải ảnh đại diện nếu có
        if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
            Glide.with(context)
                    .load(user.getProfileImage())
                    .placeholder(R.drawable.user_placeholder)
                    .into(holder.userImageView);
        }

        // Xử lý nút xóa
        holder.deleteUserBtn.setOnClickListener(v -> {
            showDeleteConfirmDialog(user);
        });

        // Xử lý nút chỉnh sửa
        holder.editUserBtn.setOnClickListener(v -> {
            showEditDialog(user);
        });
    }

    private void showDeleteConfirmDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Xóa người dùng");
        builder.setMessage("Bạn có chắc chắn muốn xóa người dùng này không?");
        
        builder.setPositiveButton("Xóa", (dialog, which) -> {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users")
                    .child(user.getId());
                    
            userRef.removeValue()
                    .addOnSuccessListener(aVoid -> {
                        int position = userList.indexOf(user);
                        if (position >= 0) {
                            userList.remove(position);
                            notifyItemRemoved(position);
                            Toast.makeText(context, "Đã xóa người dùng", Toast.LENGTH_SHORT).show();
                        } else {
                            // Người dùng không còn trong danh sách, làm mới toàn bộ adapter
                            Toast.makeText(context, "Đã xóa người dùng. Đang làm mới danh sách...", Toast.LENGTH_SHORT).show();
                            notifyDataSetChanged();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
        
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void showEditDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_edit_user, null);
        builder.setView(dialogView);

        EditText nameInput = dialogView.findViewById(R.id.editNameInput);
        EditText emailInput = dialogView.findViewById(R.id.editEmailInput);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        CircleImageView userImageView = dialogView.findViewById(R.id.editUserImageView);
        Button chooseImageButton = dialogView.findViewById(R.id.chooseImageButton);

        currentImageView = userImageView;

        // Đặt giá trị hiện tại
        nameInput.setText(user.getName());
        emailInput.setText(user.getEmail());

        // Hiển thị ảnh hiện tại
        if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
            Glide.with(context)
                    .load(user.getProfileImage())
                    .placeholder(R.drawable.user_placeholder)
                    .into(userImageView);
        }

        chooseImageButton.setOnClickListener(v -> {
            try {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                if (context instanceof Activity) {
                    ((Activity) context).startActivityForResult(
                        Intent.createChooser(intent, "Chọn ảnh"),
                        PICK_IMAGE_REQUEST
                    );
                }
            } catch (Exception e) {
                Toast.makeText(context, "Không thể mở bộ chọn ảnh", Toast.LENGTH_SHORT).show();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        saveButton.setOnClickListener(v -> {
            String newName = nameInput.getText().toString().trim();
            String newEmail = emailInput.getText().toString().trim();

            if (newName.isEmpty() || newEmail.isEmpty()) {
                Toast.makeText(context, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            // Nếu có chọn ảnh mới
            if (imageUri != null) {
                StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                    .child("profile_images/" + user.getId());
                
                storageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            user.setProfileImage(uri.toString());
                            updateUserInfo(user, newName, newEmail, dialog);
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Lỗi tải ảnh: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    });
            } else {
                updateUserInfo(user, newName, newEmail, dialog);
            }
        });

        dialog.show();
    }

    private void updateUserInfo(User user, String newName, String newEmail, AlertDialog dialog) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(user.getId());

        user.setName(newName);
        user.setEmail(newEmail);

        userRef.setValue(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    notifyDataSetChanged();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public void handleImageResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK
                && data != null && data.getData() != null) {
            imageUri = data.getData();
            if (currentImageView != null) {
                Glide.with(context)
                    .load(imageUri)
                    .into(currentImageView);
            }
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView userImageView;
        TextView userNameTextView;
        TextView userEmailTextView;
        ImageButton editUserBtn;
        ImageButton deleteUserBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userImageView = itemView.findViewById(R.id.userImageView);
            userNameTextView = itemView.findViewById(R.id.userNameTextView);
            userEmailTextView = itemView.findViewById(R.id.userEmailTextView);
            editUserBtn = itemView.findViewById(R.id.editUserBtn);
            deleteUserBtn = itemView.findViewById(R.id.deleteUserBtn);
        }
    }
}
