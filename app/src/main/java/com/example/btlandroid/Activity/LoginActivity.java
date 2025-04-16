package com.example.btlandroid.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.example.btlandroid.R;
import com.example.btlandroid.databinding.ActivityLoginBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Sửa dòng này để vô hiệu hóa reCAPTCHA trong môi trường phát triển
        FirebaseAuth.getInstance().getFirebaseAuthSettings()
                .setAppVerificationDisabledForTesting(true);

        // Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");


        // Cấu hình Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Xử lý nút đăng nhập
        binding.loginButton.setOnClickListener(v -> {
            String email = binding.emailEditText.getText().toString();
            String password = binding.passwordEditText.getText().toString();
            boolean isAdmin = binding.adminCheckBox.isChecked();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Vui lòng nhập email và mật khẩu", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(email, password, isAdmin);
            }
        });

        // Xử lý nút đăng nhập bằng Google
        binding.googleSignInButton.setOnClickListener(v -> signInWithGoogle());

        // Chuyển sang màn hình đăng ký
        binding.registerTextView.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });


        // Xử lý quên mật khẩu
        binding.forgotPasswordTextView.setOnClickListener(v -> forgotPassword());

    }

    // Kiểm tra xem người dùng đã đăng nhập hay chưa
    private void loginUser(String email, String password, boolean isAdmin) {
        binding.progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Lưu thông tin đăng nhập
                        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("email", email);
                        editor.putBoolean("isLoggedIn", true);
                        editor.apply();

                        String uid = mAuth.getCurrentUser().getUid();
                        
                        if (isAdmin) {
                            // Kiểm tra nếu người dùng là admin
                            checkAdminRole(uid);
                        } else {
                            // Đăng nhập thành công cho người dùng thường
                            Toast.makeText(LoginActivity.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        binding.progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void checkAdminRole(String uid) {
        DatabaseReference adminRef = FirebaseDatabase.getInstance().getReference("Admins").child(uid);
        
        adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                binding.progressBar.setVisibility(View.GONE);
                
                if (snapshot.exists()) {
                    // Người dùng là admin
                    Toast.makeText(LoginActivity.this, "Đăng nhập admin thành công", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, AdminActivity.class));
                } else {
                    // Không có quyền admin
                    Toast.makeText(LoginActivity.this, "Bạn không có quyền truy cập trang quản trị", Toast.LENGTH_SHORT).show();
                    mAuth.signOut(); // Đăng xuất khỏi tài khoản
                }
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(LoginActivity.this, "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Kết quả trả về từ intent của Google Sign In
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Đăng nhập Google thành công, xác thực với Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Đăng nhập Google thất bại
                Toast.makeText(this, "Đăng nhập Google thất bại", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Đăng nhập thành công
                        FirebaseUser user = mAuth.getCurrentUser();
                        
                        // Lưu thông tin người dùng vào Firebase Database
                        if (user != null) {
                            HashMap<String, Object> userMap = new HashMap<>();
                            userMap.put("name", user.getDisplayName());
                            userMap.put("email", user.getEmail());
                            userMap.put("profileImage", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");

                            mDatabase.child(user.getUid()).setValue(userMap);
                        }
                        
                        // Chuyển hướng người dùng
                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        // Đăng nhập thất bại
                        Toast.makeText(LoginActivity.this, "Xác thực thất bại.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void forgotPassword() {
        String email = binding.emailEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Vui lòng nhập email để đặt lại mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Đã gửi email đặt lại mật khẩu!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "Không thể gửi email đặt lại mật khẩu: " 
                                + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
