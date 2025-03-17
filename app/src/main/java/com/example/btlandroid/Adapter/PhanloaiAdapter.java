package com.example.btlandroid.Adapter;
import com.bumptech.glide.Glide;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btlandroid.Activity.CategoryItemsActivity;
import com.example.btlandroid.Domain.PhanloaiDomain;
import com.example.btlandroid.databinding.ViewholderPhanloaiBinding;

import java.util.ArrayList;

public class PhanloaiAdapter extends RecyclerView.Adapter<PhanloaiAdapter.ViewHolder> {
    ArrayList<PhanloaiDomain> items;
    Context context;

    public PhanloaiAdapter(ArrayList<PhanloaiDomain> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public PhanloaiAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        ViewholderPhanloaiBinding binding=ViewholderPhanloaiBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PhanloaiAdapter.ViewHolder holder, int position) {
        holder.binding.titleTxt.setText(items.get(position).getName());

        Glide.with(context)
                .load(items.get(position).getImagePath())
                .into(holder.binding.img);
                
        // Thêm xử lý sự kiện click vào item phân loại
        holder.itemView.setOnClickListener(view -> {
            // Lấy phân loại hiện tại
            PhanloaiDomain selectedCategory = items.get(position);
            
            // Tạo Intent để mở CategoryItemsActivity
            Intent intent = new Intent(context, CategoryItemsActivity.class);
            
            // Truyền thông tin phân loại qua Intent
            intent.putExtra("categoryId", selectedCategory.getId());
            intent.putExtra("categoryName", selectedCategory.getName());
            
            // Khởi chạy Activity mới
            context.startActivity(intent);
        });
        
        // Nếu có sự kiện click để mở DetailActivity, thêm debug log tương tự
        
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ViewholderPhanloaiBinding binding;
        public ViewHolder(ViewholderPhanloaiBinding binding) {
            super(binding.getRoot());
            this.binding=binding;
        }
    }
}
