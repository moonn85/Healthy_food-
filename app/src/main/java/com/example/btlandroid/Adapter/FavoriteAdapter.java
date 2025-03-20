package com.example.btlandroid.Adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.btlandroid.Activity.DetailActivity;
import com.example.btlandroid.Domain.FavoriteItem;
import com.example.btlandroid.Domain.ItemDomain;
import com.example.btlandroid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DecimalFormat;
import java.util.List;

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.ViewHolder> {
    private List<FavoriteItem> favoriteItems;
    private Context context;
    private FirebaseUser currentUser;
    private OnItemRemovedListener onItemRemovedListener;

    public interface OnItemRemovedListener {
        void onItemRemoved();
    }

    public FavoriteAdapter(List<FavoriteItem> favoriteItems, OnItemRemovedListener listener) {
        this.favoriteItems = favoriteItems;
        this.onItemRemovedListener = listener;
        this.currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.viewholder_favorite, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FavoriteItem item = favoriteItems.get(position);
        
        // Hiển thị thông tin sản phẩm
        holder.titleTxt.setText(item.getTitle());
        
        // Định dạng giá tiền
        DecimalFormat formatter = new DecimalFormat("#,###");
        holder.priceTxt.setText(formatter.format(item.getPrice()) + " VND/Kg");
        
        // Tải ảnh sản phẩm
        Glide.with(context)
                .load(item.getPicUrl()) // Use getPicUrl() instead of getImagePath()
                .placeholder(R.drawable.light_black_bg)
                .error(R.drawable.grey_button_bg)
                .into(holder.productImg);
        
        // Xử lý sự kiện khi nhấn vào sản phẩm
        holder.itemView.setOnClickListener(v -> {
            // Tạo ItemDomain mới từ FavoriteItem để đảm bảo tương thích với DetailActivity
            ItemDomain itemDomain = new ItemDomain();
            itemDomain.setId(Integer.parseInt(item.getProductId())); // Use productId and convert to int
            itemDomain.setTitle(item.getTitle());
            itemDomain.setPrice(item.getPrice());
            itemDomain.setImagePath(item.getPicUrl()); // Use getPicUrl() instead of getImagePath()
            
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("item", itemDomain); // Truyền object ItemDomain
            
            // Thêm cả các thông tin riêng lẻ để đảm bảo tương thích với cả 2 cách
            intent.putExtra("itemId", Integer.parseInt(item.getProductId())); // Use productId and convert to int
            intent.putExtra("title", item.getTitle());
            intent.putExtra("price", item.getPrice());
            intent.putExtra("imagePath", item.getPicUrl()); // Use getPicUrl() instead of getImagePath()
            
            context.startActivity(intent);
        });
        
        // Xử lý sự kiện khi nhấn nút xóa
        holder.removeBtn.setOnClickListener(v -> {
            removeFromFavorite(holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return favoriteItems.size();
    }

    /**
     * Xóa sản phẩm khỏi danh sách yêu thích trong Firebase
     */
    private void removeFromFavorite(int position) {
        if (currentUser == null || position < 0 || position >= favoriteItems.size()) return;
        
        FavoriteItem item = favoriteItems.get(position);
        
        // Xóa từ Firebase
        DatabaseReference favoriteRef = FirebaseDatabase.getInstance().getReference("Favorites")
                .child(currentUser.getUid())
                .child(item.getProductId()); // Use getProductId() instead of getId()
        
        favoriteRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Xóa khỏi danh sách hiển thị
                if (!favoriteItems.isEmpty() && position >= 0 && position < favoriteItems.size()) {
                    favoriteItems.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, favoriteItems.size());
                } else {
                    Log.e("FavoriteAdapter", "Attempted to remove item from empty list or invalid position");
                }
                
                // Thông báo cho Activity về thay đổi
                if (onItemRemovedListener != null) {
                    onItemRemovedListener.onItemRemoved();
                }
                
                Toast.makeText(context, "Đã xóa khỏi danh sách yêu thích", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Không thể xóa sản phẩm", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTxt, priceTxt;
        ImageView productImg, removeBtn;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTxt = itemView.findViewById(R.id.favItemTitleTxt);
            priceTxt = itemView.findViewById(R.id.favItemPriceTxt);
            productImg = itemView.findViewById(R.id.favItemImg);
            removeBtn = itemView.findViewById(R.id.removeFavBtn);
        }
    }
}
