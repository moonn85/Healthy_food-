package com.example.btlandroid.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.btlandroid.Domain.CartItem;
import com.example.btlandroid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DecimalFormat;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

    private List<CartItem> cartItems;
    private Context context;
    private OnCartUpdateListener listener;
    private DecimalFormat formatter = new DecimalFormat("#,###");

    public interface OnCartUpdateListener {
        void onCartUpdated();
    }

    public CartAdapter(List<CartItem> cartItems, Context context, OnCartUpdateListener listener) {
        this.cartItems = cartItems;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_cart_item, parent, false);
        return new ViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        
        // Hiển thị thông tin sản phẩm
        holder.titleTxt.setText(item.getTitle());
        holder.priceTxt.setText(formatter.format(item.getPrice()) + " đ");
        holder.quantityTxt.setText(String.valueOf(item.getQuantity()));
        holder.totalPriceTxt.setText(formatter.format(item.getTotalPrice()) + " đ");
        
        // Tải hình ảnh sản phẩm
        Glide.with(context)
                .load(item.getImagePath())
                .placeholder(R.drawable.light_black_bg)
                .error(R.drawable.grey_button_bg)
                .into(holder.productImg);
        
        // Xử lý sự kiện giảm số lượng
        holder.minusBtn.setOnClickListener(v -> {
            if (item.getQuantity() > 1) {
                updateItemQuantity(item, item.getQuantity() - 1);
            }
        });
        
        // Xử lý sự kiện tăng số lượng
        holder.plusBtn.setOnClickListener(v -> {
            updateItemQuantity(item, item.getQuantity() + 1);
        });
        
        // Xử lý sự kiện xóa sản phẩm
        holder.removeBtn.setOnClickListener(v -> {
            removeCartItem(item);
        });
    }

    private void updateItemQuantity(CartItem item, int newQuantity) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference("Cart")
                .child(userId)
                .child(String.valueOf(item.getId()));
        
        // Cập nhật số lượng trên Firebase
        cartRef.child("quantity").setValue(newQuantity)
                .addOnSuccessListener(unused -> {
                    item.setQuantity(newQuantity);
                    notifyDataSetChanged();
                    if (listener != null) {
                        listener.onCartUpdated();
                    }
                });
    }

    private void removeCartItem(CartItem item) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference("Cart")
                .child(userId)
                .child(String.valueOf(item.getId()));
        
        // Xóa sản phẩm khỏi giỏ hàng
        cartRef.removeValue()
                .addOnSuccessListener(unused -> {
                    int position = cartItems.indexOf(item);
                    if (position != -1) {
                        cartItems.remove(position);
                        notifyItemRemoved(position);
                        if (listener != null) {
                            listener.onCartUpdated();
                        }
                    }
                });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTxt, priceTxt, quantityTxt, totalPriceTxt;
        ImageView productImg;
        ImageButton minusBtn, plusBtn, removeBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTxt = itemView.findViewById(R.id.cartItemTitleTxt);
            priceTxt = itemView.findViewById(R.id.cartItemPriceTxt);
            quantityTxt = itemView.findViewById(R.id.quantityTxt);
            totalPriceTxt = itemView.findViewById(R.id.totalItemPriceTxt);
            productImg = itemView.findViewById(R.id.cartItemImg);
            minusBtn = itemView.findViewById(R.id.minusBtn);
            plusBtn = itemView.findViewById(R.id.plusBtn);
            removeBtn = itemView.findViewById(R.id.removeCartItemBtn);
        }
    }
}
