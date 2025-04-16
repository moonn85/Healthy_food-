package com.example.btlandroid.Adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.btlandroid.Activity.ProductEditActivity;
import com.example.btlandroid.Domain.ProductDomain;
import com.example.btlandroid.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.DecimalFormat;
import java.util.ArrayList;

// Lớp ProductAdminAdapter để hiển thị danh sách sản phẩm trong ứng dụng Android
public class ProductAdminAdapter extends RecyclerView.Adapter<ProductAdminAdapter.ViewHolder> {

    private ArrayList<ProductDomain> products;
    private Context context;
    private DatabaseReference databaseRef; // Thay đổi từ Firestore sang Database
    private FirebaseStorage storage;

    public ProductAdminAdapter(ArrayList<ProductDomain> products, Context context) {
        this.products = products;
        this.context = context;
        this.databaseRef = FirebaseDatabase.getInstance().getReference("Items");
        this.storage = FirebaseStorage.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_admin, parent, false);
        return new ViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductDomain product = products.get(position);
        
        // Định dạng giá tiền
        DecimalFormat formatter = new DecimalFormat("#,###");
        String formattedPrice = formatter.format(product.getFee());
        
        // Hiển thị thông tin sản phẩm
        holder.titleTxt.setText(product.getTitle());
        holder.priceTxt.setText(formattedPrice + " đ");
        holder.ratingTxt.setText(String.valueOf(product.getScore()));
        holder.categoryText.setText(product.getCategoryType());
        
        // Tải ảnh sản phẩm
        int drawableResourceId = holder.itemView.getContext().getResources()
                .getIdentifier(product.getPic(), "drawable", holder.itemView.getContext().getPackageName());
        
        // Kiểm tra nếu ảnh từ resource local hoặc từ URL
        if (product.getPic().startsWith("http")) {
            Glide.with(holder.itemView.getContext())
                    .load(product.getPic())
                    .into(holder.productImg);
        } else {
            Glide.with(holder.itemView.getContext())
                    .load(drawableResourceId)
                    .into(holder.productImg);
        }
        
        // Sự kiện khi nhấn nút chỉnh sửa
        holder.editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ProductEditActivity.class);
                intent.putExtra("isNewProduct", false);
                intent.putExtra("productId", product.getProductId());
                context.startActivity(intent);
            }
        });
        
        // Sự kiện khi nhấn nút xóa
        holder.deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteConfirmDialog(product);
            }
        });
    }

    private void showDeleteConfirmDialog(ProductDomain product) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context); // Removed R.style.AlertDialogTheme
        builder.setTitle("Xóa sản phẩm");
        builder.setMessage("Bạn có chắc chắn muốn xóa sản phẩm này không?");
        
        builder.setPositiveButton("Xóa", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteProduct(product);
            }
        });
        
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void deleteProduct(ProductDomain product) {
        // Xóa sản phẩm khỏi Realtime Database thay vì Firestore
        databaseRef.child(product.getProductId())
                .removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Xóa ảnh nếu có
                        if (product.getPic().startsWith("http")) {
                            try {
                                // Lấy tham chiếu đến file trong storage
                                StorageReference storageRef = storage.getReferenceFromUrl(product.getPic());
                                storageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        // Xóa thành công
                                    }
                                });
                            } catch (Exception e) {
                                // Xử lý lỗi khi không thể xóa ảnh
                            }
                        }
                        
                        // Xóa khỏi danh sách và cập nhật adapter
                        int position = products.indexOf(product);
                        if (position != -1) {
                            products.remove(position);
                            notifyItemRemoved(position);
                        }
                        
                        Toast.makeText(context, "Xóa sản phẩm thành công", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context, "Lỗi khi xóa sản phẩm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTxt, priceTxt, ratingTxt, categoryText;
        ImageView productImg, starImg;
        ImageButton editBtn, deleteBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTxt = itemView.findViewById(R.id.titleTxt);
            priceTxt = itemView.findViewById(R.id.priceTxt);
            productImg = itemView.findViewById(R.id.productImg);
            ratingTxt = itemView.findViewById(R.id.ratingTxt);
            starImg = itemView.findViewById(R.id.starImg);
            categoryText = itemView.findViewById(R.id.categoryText);
            editBtn = itemView.findViewById(R.id.editProductBtn);
            deleteBtn = itemView.findViewById(R.id.deleteProductBtn);
        }
    }
}
