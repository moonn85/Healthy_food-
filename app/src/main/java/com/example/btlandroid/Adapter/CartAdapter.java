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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DecimalFormat;
import java.util.List;

// Đây là một lớp CartAdapter trong ứng dụng Android, được sử dụng để hiển thị danh sách sản phẩm trong giỏ hàng của người dùng.
// Nó kế thừa từ RecyclerView.Adapter và sử dụng ViewHolder để quản lý các item trong danh sách giỏ hàng.
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

    // Phương thức này được gọi khi RecyclerView cần tạo một ViewHolder mới để hiển thị một item trong danh sách
    // Nó sẽ tạo một ViewHolder mới từ layout viewholder_cart_item.xml và trả về nó.
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
                .load(item.getPicUrl()) // Sử dụng getPicUrl thay vì getImagePath
                .placeholder(R.drawable.light_black_bg)
                .error(R.drawable.grey_button_bg)
                .into(holder.productImg);
        
        // Xử lý sự kiện giảm số lượng
        holder.minusBtn.setOnClickListener(v -> {
            if (item.getQuantity() > 1) {
                int newQuantity = item.getQuantity() - 1;
                item.setQuantity(newQuantity);
                // Cập nhật tổng giá dựa trên số lượng mới
                item.updateTotalPrice();
                // Cập nhật trên Firebase
                updateItemInDatabase(item);
                // Thông báo cho CartActivity cập nhật tổng tiền
                listener.onCartUpdated();
                notifyItemChanged(position);
            }
        });
        
        // Xử lý sự kiện tăng số lượng
        holder.plusBtn.setOnClickListener(v -> {
            int newQuantity = item.getQuantity() + 1;
            item.setQuantity(newQuantity);
            // Cập nhật tổng giá dựa trên số lượng mới
            item.updateTotalPrice();
            // Cập nhật trên Firebase
            updateItemInDatabase(item);
            // Thông báo cho CartActivity cập nhật tổng tiền
            listener.onCartUpdated();
            notifyItemChanged(position);
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
                .child(item.getProductId()); // Sử dụng productId thay vì getId
        
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
                .child(item.getProductId()); // Sử dụng productId thay vì getId
        
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
// Cập nhật thông tin sản phẩm trong Firebase
    private void updateItemInDatabase(CartItem item) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference("Cart")
                    .child(currentUser.getUid()).child(item.getProductId());
            cartRef.setValue(item);
        }
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
