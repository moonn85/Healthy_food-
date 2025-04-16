package com.example.btlandroid.Activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.example.btlandroid.Domain.ProductDomain;
import com.example.btlandroid.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

//để thêm hoặc chỉnh sửa thông tin sản phẩm
// sử dụng Firebase Realtime Database để lưu trữ thông tin sản phẩm
public class ProductEditActivity extends AppCompatActivity {

    private ImageButton backBtn;
    private Button saveBtn;
    private TextView titleHeader, imagePickerText, ratingValueText;
    private ImageView productImageView;
    private CardView imagePickerCard;
    private TextInputEditText titleEditText, descriptionEditText, priceEditText, stockEditText;
    private Spinner categorySpinner, locationSpinner;
    private RatingBar productRatingBar;
    
    private DatabaseReference databaseRef; // Thay đổi từ Firestore sang Database
    private FirebaseStorage storage;
    private StorageReference storageReference;
    
    private Uri imageUri;
    private String currentImageUrl;
    private boolean isNewProduct;
    private String productId;
    private boolean imageChanged = false;
    
    // Sửa lại mảng danh mục
    private final String[] categories = {"Rau", "Hoa quả", "Sữa", "Đồ uống", "Hạt", "Khác"};
    // Sửa lại mảng vị trí
    private final String[] locations = {"Việt Nam", "Singapore", "Không xác định"};
    
    private AlertDialog progressDialog;

    // ActivityResultLauncher để chọn ảnh từ thư viện
    private ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri result) {
                    if (result != null) {
                        imageUri = result;
                        imageChanged = true;
                        imagePickerText.setVisibility(View.GONE);
                        Glide.with(ProductEditActivity.this)
                                .load(result)
                                .into(productImageView);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_edit);
        
        // Khởi tạo Firebase Realtime Database thay vì Firestore
        databaseRef = FirebaseDatabase.getInstance().getReference("Items");
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        
        // Lấy thông tin từ intent
        Intent intent = getIntent();
        isNewProduct = intent.getBooleanExtra("isNewProduct", true);
        if (!isNewProduct) {
            productId = intent.getStringExtra("productId");
        }
        
        // Ánh xạ view
        initView();
        
        // Thiết lập spinner
        setupSpinners();
        
        // Thiết lập listener cho RatingBar
        productRatingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            ratingValueText.setText(String.valueOf(rating));
        });
        
        // Xử lý click vào card chọn ảnh
        imagePickerCard.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        
        // Xử lý nút quay lại
        backBtn.setOnClickListener(v -> finish());
        
        // Xử lý nút lưu
        saveBtn.setOnClickListener(v -> validateAndSaveProduct());
        
        // Cập nhật UI dựa vào chế độ thêm mới hay chỉnh sửa
        if (isNewProduct) {
            titleHeader.setText("Thêm sản phẩm");
        } else {
            titleHeader.setText("Sửa sản phẩm");
            loadProductDetails();
        }
    }

    private void initView() {
        backBtn = findViewById(R.id.backBtn);
        saveBtn = findViewById(R.id.saveBtn);
        titleHeader = findViewById(R.id.titleHeader);
        imagePickerText = findViewById(R.id.imagePickerText);
        productImageView = findViewById(R.id.productImageView);
        imagePickerCard = findViewById(R.id.imagePickerCard);
        titleEditText = findViewById(R.id.titleEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        priceEditText = findViewById(R.id.priceEditText);
        categorySpinner = findViewById(R.id.categorySpinner);
        locationSpinner = findViewById(R.id.locationSpinner);
        productRatingBar = findViewById(R.id.productRatingBar);
        ratingValueText = findViewById(R.id.ratingValueText);
        stockEditText = findViewById(R.id.stockEditText);
    }

    private void setupSpinners() {
        // Adapter cho spinner danh mục
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);
        
        // Adapter cho spinner vị trí
        ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, locations);
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(locationAdapter);
    }

    private void loadProductDetails() {
        // Tạo AlertDialog thay thế cho ProgressDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_loading, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        TextView messageTextView = dialogView.findViewById(R.id.messageTextView);
        messageTextView.setText("Đang tải thông tin sản phẩm...");
        progressDialog = builder.create();
        progressDialog.show();
        
        // Lấy thông tin sản phẩm từ Realtime Database
        databaseRef.child(productId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressDialog.dismiss();
                
                if (snapshot.exists()) {
                    // Lấy dữ liệu sản phẩm
                    String title = snapshot.child("Title").getValue(String.class);
                    String description = snapshot.child("Description").getValue(String.class);
                    Double price = snapshot.child("Price").getValue(Double.class);
                    Integer categoryId = snapshot.child("CategoryId").getValue(Integer.class);
                    Double score = snapshot.child("Star").getValue(Double.class);
                    Integer locationId = snapshot.child("LocationId").getValue(Integer.class);
                    String pic = snapshot.child("ImagePath").getValue(String.class);
                    
                    // Hiển thị dữ liệu lên form
                    titleEditText.setText(title);
                    descriptionEditText.setText(description);
                    priceEditText.setText(String.valueOf(price.intValue()));
                    stockEditText.setText("10"); // Mặc định là 10
                    
                    // Thiết lập spinner danh mục
                    if (categoryId != null && categoryId >= 0 && categoryId < categories.length) {
                        categorySpinner.setSelection(categoryId);
                    }
                    
                    // Thiết lập spinner vị trí
                    if (locationId != null) {
                        int locationPos = (locationId == 1) ? 0 : (locationId == 0 ? 1 : 2);
                        locationSpinner.setSelection(locationPos);
                    }
                    
                    // Thiết lập rating
                    if (score != null) {
                        productRatingBar.setRating(score.floatValue());
                        ratingValueText.setText(String.valueOf(score));
                    }
                    
                    // Hiển thị ảnh sản phẩm
                    currentImageUrl = pic;
                    if (pic != null && pic.startsWith("http")) {
                        imagePickerText.setVisibility(View.GONE);
                        Glide.with(ProductEditActivity.this)
                            .load(pic)
                            .into(productImageView);
                    }
                } else {
                    Toast.makeText(ProductEditActivity.this, 
                        "Không tìm thấy sản phẩm", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Toast.makeText(ProductEditActivity.this, 
                    "Lỗi khi tải thông tin sản phẩm", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void validateAndSaveProduct() {
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String priceStr = priceEditText.getText().toString().trim();
        String stockStr = stockEditText.getText().toString().trim();
        String category = categorySpinner.getSelectedItem().toString();
        String location = locationSpinner.getSelectedItem().toString();
        float rating = productRatingBar.getRating();
        
        // Kiểm tra dữ liệu nhập
        if (title.isEmpty()) {
            titleEditText.setError("Vui lòng nhập tên sản phẩm");
            return;
        }
        
        if (description.isEmpty()) {
            descriptionEditText.setError("Vui lòng nhập mô tả sản phẩm");
            return;
        }
        
        if (priceStr.isEmpty()) {
            priceEditText.setError("Vui lòng nhập giá sản phẩm");
            return;
        }
        
        if (stockStr.isEmpty()) {
            stockEditText.setError("Vui lòng nhập số lượng");
            return;
        }
        
        double price = Double.parseDouble(priceStr);
        int stock = Integer.parseInt(stockStr);
        
        // Kiểm tra có ảnh chưa khi thêm mới
        if (isNewProduct && imageUri == null) {
            Toast.makeText(this, "Vui lòng chọn ảnh cho sản phẩm", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Tạo AlertDialog thay thế cho ProgressDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_loading, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        TextView messageTextView = dialogView.findViewById(R.id.messageTextView);
        messageTextView.setText("Đang lưu sản phẩm...");
        progressDialog = builder.create();
        progressDialog.show();
        
        // Nếu có cập nhật ảnh mới, tải ảnh lên trước
        if (imageChanged) {
            uploadImageAndSaveProduct(title, description, price, category, location, rating, stock, progressDialog);
        } else {
            // Không có ảnh mới, lưu với ảnh hiện tại
            saveProductToFirestore(title, description, price, category, location, rating, stock, 
                currentImageUrl != null ? currentImageUrl : "", progressDialog);
        }
    }

    private void uploadImageAndSaveProduct(String title, String description, double price,
                                          String category, String location, float rating, 
                                          int stock, AlertDialog progressDialog) {
        // Tạo tên file ngẫu nhiên cho ảnh
        String randomFileName = UUID.randomUUID().toString();
        StorageReference imageRef = storageReference.child("product_images/" + randomFileName);
        
        // Tải ảnh lên Firebase Storage
        imageRef.putFile(imageUri)
            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // Lấy URL tải xuống của ảnh đã tải lên
                    imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String imageUrl = uri.toString();
                            
                            // Lưu thông tin sản phẩm với URL ảnh mới
                            saveProductToFirestore(title, description, price, category, 
                                location, rating, stock, imageUrl, progressDialog);
                        }
                    });
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(ProductEditActivity.this, 
                        "Lỗi khi tải ảnh lên: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void saveProductToFirestore(String title, String description, double price,
                                      String category, String location, float rating,
                                      int stock, String imageUrl, AlertDialog progressDialog) {
        // Chuẩn bị dữ liệu để lưu vào Realtime Database
        Map<String, Object> productData = new HashMap<>();
        productData.put("Title", title);
        productData.put("Description", description);
        productData.put("Price", price);
        
        // Chuyển đổi từ tên category sang CategoryId
        int categoryId = 0;
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(category)) {
                categoryId = i;
                break;
            }
        }
        productData.put("CategoryId", categoryId);
        
        // Chuyển đổi từ tên location sang LocationId
        int locationId = 1; // Mặc định là Việt Nam
        if (location.equals("Singapore")) {
            locationId = 0;
        }
        productData.put("LocationId", locationId);
        
        productData.put("Star", (double) rating);
        productData.put("ImagePath", imageUrl);
        
        if (isNewProduct) {
            // Tạo Id mới cho sản phẩm
            Long newId = Long.parseLong(String.valueOf(System.currentTimeMillis()));
            productData.put("Id", newId);
            
            // Lưu sản phẩm vào Realtime Database
            databaseRef.child(String.valueOf(newId)).setValue(productData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        progressDialog.dismiss();
                        Toast.makeText(ProductEditActivity.this, 
                            "Thêm sản phẩm thành công", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(ProductEditActivity.this, 
                            "Lỗi khi thêm sản phẩm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
        } else {
            // Cập nhật sản phẩm hiện có
            databaseRef.child(productId).updateChildren(productData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        progressDialog.dismiss();
                        Toast.makeText(ProductEditActivity.this, 
                            "Cập nhật sản phẩm thành công", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(ProductEditActivity.this, 
                            "Lỗi khi cập nhật sản phẩm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
        }
    }
}
