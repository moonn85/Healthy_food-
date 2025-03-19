package com.example.btlandroid.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btlandroid.Domain.CartItem;
import com.example.btlandroid.Domain.OrderDomain;
import com.example.btlandroid.Activity.OrderDetailActivity;
import com.example.btlandroid.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {
    private List<OrderDomain> orderList;
    private Context context;
    private boolean isAdmin;
    
    public OrderAdapter(List<OrderDomain> orderList) {
        this.orderList = orderList;
        this.isAdmin = false;
    }
    
    public OrderAdapter(List<OrderDomain> orderList, Context context, boolean isAdmin) {
        this.orderList = orderList;
        this.context = context;
        this.isAdmin = isAdmin;
    }
    
    /**
     * Cập nhật danh sách đơn hàng và làm mới RecyclerView
     * 
     * @param orderList Danh sách đơn hàng mới
     */
    public void setOrderList(List<OrderDomain> orderList) {
        this.orderList = orderList;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderDomain order = orderList.get(position);
        Context context = holder.itemView.getContext(); // Get context from itemView
        
        // Hiển thị thông tin đơn hàng
        holder.orderIdTxt.setText("Mã đơn hàng: " + order.getOrderId());
        
        // Xử lý ngày đặt, tránh null
        String orderDate = order.getOrderDate();
        holder.orderDateTxt.setText("Ngày đặt: " + (orderDate != null ? orderDate : "Chưa có"));
        
        holder.statusTxt.setText("Trạng thái: " + order.getStatus());
        
        // Định dạng số tiền với đơn vị VND
        NumberFormat currencyFormatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        double totalAmount = order.getTotalAmount();
        
        // Đảm bảo hiển thị tổng tiền, nếu là 0 thì hiển thị thông báo
        if (totalAmount > 0) {
            holder.totalAmountTxt.setText("Tổng tiền: " + currencyFormatter.format(totalAmount) + " VNĐ");
        } else {
            // Nếu không có sẵn tổng tiền, tính lại từ các mặt hàng
            double calculatedTotal = 0;
            if (order.getCartItems() != null) {
                for (CartItem item : order.getCartItems().values()) {
                    if (item != null) {
                        calculatedTotal += item.getPrice() * item.getQuantity();
                    }
                }
            }
            
            if (calculatedTotal > 0) {
                holder.totalAmountTxt.setText("Tổng tiền: " + currencyFormatter.format(calculatedTotal) + " VNĐ");
            } else {
                holder.totalAmountTxt.setText("Tổng tiền: Không có thông tin");
            }
        }
        
        // Thêm xử lý click vào nút xem chi tiết
        holder.viewDetailBtn.setOnClickListener(v -> {
            if (context != null) {
                Intent intent = new Intent(context, OrderDetailActivity.class);
                intent.putExtra("orderId", order.getOrderId());
                context.startActivity(intent);
            }
        });

        // Hiển thị/Ẩn các nút cho admin
        if (isAdmin) {
            holder.adminButtonsLayout.setVisibility(View.VISIBLE);
            setupAdminButtons(holder, order);
        } else {
            holder.adminButtonsLayout.setVisibility(View.GONE);
        }
    }
    
    private void setupAdminButtons(ViewHolder holder, OrderDomain order) {
        // Xử lý sự kiện cập nhật trạng thái đơn hàng
        holder.confirmBtn.setOnClickListener(v -> updateOrderStatus(order, "Đã xác nhận"));
        holder.shippingBtn.setOnClickListener(v -> updateOrderStatus(order, "Đang giao hàng"));
        holder.deliveredBtn.setOnClickListener(v -> updateOrderStatus(order, "Đã giao hàng"));
        holder.cancelBtn.setOnClickListener(v -> updateOrderStatus(order, "Đã hủy"));
    }
    
    private void updateOrderStatus(OrderDomain order, String newStatus) {
        if (context == null) return;
        
        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference("Orders")
                .child(order.getOrderId());
        
        orderRef.child("status").setValue(newStatus)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(context, "Cập nhật trạng thái thành công", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    @Override
    public int getItemCount() {
        return orderList.size();
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderIdTxt, orderDateTxt, statusTxt, totalAmountTxt;
        View adminButtonsLayout;
        Button confirmBtn, shippingBtn, deliveredBtn, cancelBtn, viewDetailBtn;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            orderIdTxt = itemView.findViewById(R.id.orderIdTxt);
            orderDateTxt = itemView.findViewById(R.id.orderDateTxt);
            statusTxt = itemView.findViewById(R.id.statusTxt);
            totalAmountTxt = itemView.findViewById(R.id.totalAmountTxt);
            
            // Admin buttons layout (có thể ẩn dựa vào quyền)
            adminButtonsLayout = itemView.findViewById(R.id.adminButtonsLayout);
            confirmBtn = itemView.findViewById(R.id.confirmBtn);
            shippingBtn = itemView.findViewById(R.id.shippingBtn);
            deliveredBtn = itemView.findViewById(R.id.deliveredBtn);
            cancelBtn = itemView.findViewById(R.id.cancelBtn);
            viewDetailBtn = itemView.findViewById(R.id.viewDetailBtn);
        }
    }
}
