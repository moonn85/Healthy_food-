package com.example.btlandroid.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.btlandroid.Activity.DetailActivity;
import com.example.btlandroid.Domain.ItemDomain;
import com.example.btlandroid.R;
import com.example.btlandroid.databinding.ViewholderBestdealBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class BestdealAdapter extends RecyclerView.Adapter<BestdealAdapter.ViewHolder> {
    ArrayList<ItemDomain> items;
    Context context;
    private FirebaseUser currentUser;

    public BestdealAdapter(ArrayList<ItemDomain> items) {
        this.items = items;
        this.currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @NonNull
    @Override
    public BestdealAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context=parent.getContext();
        ViewholderBestdealBinding binding=ViewholderBestdealBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemDomain item = items.get(position);
        holder.binding.TitleTxt.setText(item.getTitle());
        holder.binding.priceTxt.setText(item.getPrice() + "Vnd/Kg");
        Glide.with(context)
                .load(item.getImagePath())
                .into(holder.binding.img);
                
        // Kiểm tra xem sản phẩm có trong danh sách yêu thích không
        checkIfFavorite(item.getId(), holder);

        // Xử lý sự kiện click vào icon yêu thích
        holder.binding.favBtn.setOnClickListener(v -> {
            toggleFavorite(item, holder);
        });
                
        // Thêm sự kiện click vào item
        holder.itemView.setOnClickListener(v -> {
            try {
                // Log để debug
                Log.d("BestdealAdapter", "Clicked on item: " + items.get(position).getTitle());
                
                Intent intent = new Intent(context, DetailActivity.class);
                intent.putExtra("item", items.get(position));
                context.startActivity(intent);
            } catch (Exception e) {
                Log.e("BestdealAdapter", "Error starting DetailActivity: " + e.getMessage(), e);
                Toast.makeText(context, "Không thể mở chi tiết sản phẩm", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Kiểm tra xem sản phẩm có trong danh sách yêu thích không
     */
    private void checkIfFavorite(int itemId, ViewHolder holder) {
        if (currentUser == null) return;

        DatabaseReference favoriteRef = FirebaseDatabase.getInstance().getReference("Favorites")
                .child(currentUser.getUid())
                .child(String.valueOf(itemId));

        favoriteRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Sản phẩm đã được yêu thích
                    holder.binding.favBtn.setImageResource(R.drawable.favorite_green);
                } else {
                    // Sản phẩm chưa được yêu thích
                    holder.binding.favBtn.setImageResource(R.drawable.favorite_border);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Xử lý lỗi nếu cần
            }
        });
    }

    /**
     * Thêm/xóa sản phẩm khỏi danh sách yêu thích
     */
    private void toggleFavorite(ItemDomain item, ViewHolder holder) {
        if (currentUser == null) {
            Toast.makeText(context, "Vui lòng đăng nhập để thêm vào yêu thích", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference favoriteRef = FirebaseDatabase.getInstance().getReference("Favorites")
                .child(currentUser.getUid())
                .child(String.valueOf(item.getId()));

        favoriteRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Nếu đã yêu thích, xóa khỏi danh sách yêu thích
                    favoriteRef.removeValue().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            holder.binding.favBtn.setImageResource(R.drawable.favorite_border);
                            Toast.makeText(context, "Đã xóa khỏi danh sách yêu thích", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // Nếu chưa yêu thích, thêm vào danh sách yêu thích
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    String timestamp = sdf.format(new Date());

                    HashMap<String, Object> favoriteMap = new HashMap<>();
                    favoriteMap.put("id", item.getId());
                    favoriteMap.put("title", item.getTitle());
                    favoriteMap.put("price", item.getPrice());
                    favoriteMap.put("imagePath", item.getImagePath());
                    favoriteMap.put("timestamp", timestamp);

                    favoriteRef.setValue(favoriteMap).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            holder.binding.favBtn.setImageResource(R.drawable.favorite_green);
                            Toast.makeText(context, "Đã thêm vào danh sách yêu thích", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ViewholderBestdealBinding binding;
        public ViewHolder(ViewholderBestdealBinding binding) {
            super(binding.getRoot());
            this.binding=binding;
        }
    }
}
