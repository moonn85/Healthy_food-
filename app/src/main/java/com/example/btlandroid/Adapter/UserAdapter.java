package com.example.btlandroid.Adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.btlandroid.Model.Message;
import com.example.btlandroid.Model.User;
import com.example.btlandroid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {
    private Context context;
    private List<User> users;
    private OnUserClickListener listener;

    public UserAdapter(Context context, List<User> users, OnUserClickListener listener) {
        this.context = context;
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        holder.userName.setText(user.getName());

        if (user.getProfileImage() != null) {
            Glide.with(context)
                .load(user.getProfileImage())
                .placeholder(R.drawable.user_placeholder)
                .into(holder.userAvatar);
        }

        // Load tin nhắn cuối cùng
        loadLastMessage(holder, user.getId());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(user);
            }
        });
    }

    private void loadLastMessage(ViewHolder holder, String userId) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("Chats");
        
        // Tạo chatRoomId để lấy đúng cuộc hội thoại
        String chatRoomId = getChatRoomId(currentUserId, userId);
        
        // Query tin nhắn cuối cùng của cuộc hội thoại này
        chatRef.child(chatRoomId)
                .orderByChild("timestamp")
                .limitToLast(1)
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Message message = dataSnapshot.getValue(Message.class);
                    if (message != null) {
                        // Hiển thị nội dung tin nhắn
                        String content = message.hasImage() ? "[Hình ảnh]" : message.getContent();
                        if (content != null && content.length() > 30) {
                            content = content.substring(0, 27) + "...";
                        }
                        holder.lastMessage.setText(content);

                        // Format thời gian
                        long timestamp = message.getTimestamp();
                        String time = getFormattedTime(timestamp);
                        holder.lastMessageTime.setText(time);

                        // Set style cho tin nhắn chưa đọc
                        if (!message.isRead() && message.getReceiverId().equals(currentUserId)) {
                            holder.lastMessage.setTypeface(holder.lastMessage.getTypeface(), Typeface.BOLD);
                            holder.lastMessage.setTextColor(context.getResources().getColor(R.color.yellow));
                        } else {
                            holder.lastMessage.setTypeface(null, Typeface.NORMAL);
                            holder.lastMessage.setTextColor(context.getResources().getColor(R.color.white));
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private String getChatRoomId(String user1, String user2) {
        if (user1.compareTo(user2) > 0) {
            return user1 + "_" + user2;
        } else {
            return user2 + "_" + user1;
        }
    }

    private String getFormattedTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diffTime = now - timestamp;
        long seconds = diffTime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " ngày";
        } else if (hours > 0) {
            return hours + " giờ";
        } else if (minutes > 0) {
            return minutes + " phút";
        } else {
            return "Vừa xong";
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView userAvatar;
        TextView userName, lastMessage, lastMessageTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userAvatar = itemView.findViewById(R.id.userAvatar);
            userName = itemView.findViewById(R.id.userName);
            lastMessage = itemView.findViewById(R.id.lastMessage);
            lastMessageTime = itemView.findViewById(R.id.lastMessageTime);
        }
    }

    public interface OnUserClickListener {
        void onUserClick(User user);
    }
}
