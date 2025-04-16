package com.example.btlandroid.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.btlandroid.Model.TopProduct;
import com.example.btlandroid.R;

import java.text.DecimalFormat;
import java.util.List;

// Lớp TopProductAdapter để hiển thị danh sách sản phẩm hàng đầu trong ứng dụng Android
public class TopProductAdapter extends RecyclerView.Adapter<TopProductAdapter.ViewHolder> {

    private List<TopProduct> topProducts;
    private Context context;
    private DecimalFormat formatter = new DecimalFormat("#,###");

    public TopProductAdapter(Context context, List<TopProduct> topProducts) {
        this.context = context;
        this.topProducts = topProducts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_top_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TopProduct product = topProducts.get(position);
        
        // Hiển thị thông tin sản phẩm
        holder.rankTextView.setText(String.valueOf(position + 1));
        holder.productNameTextView.setText(product.getProductName());
        holder.soldQuantityTextView.setText("Đã bán: " + product.getQuantitySold());
        holder.revenueTextView.setText(formatter.format(product.getRevenue()) + " đ");

        // Tải hình ảnh sản phẩm
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.light_black_bg)
                    .error(R.drawable.grey_button_bg)
                    .into(holder.productImageView);
        }
    }

    @Override
    public int getItemCount() {
        return topProducts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView rankTextView, productNameTextView, soldQuantityTextView, revenueTextView;
        ImageView productImageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            rankTextView = itemView.findViewById(R.id.rankTextView);
            productNameTextView = itemView.findViewById(R.id.productNameTextView);
            soldQuantityTextView = itemView.findViewById(R.id.soldQuantityTextView);
            revenueTextView = itemView.findViewById(R.id.revenueTextView);
            productImageView = itemView.findViewById(R.id.productImageView);
        }
    }
}
