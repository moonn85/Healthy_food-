package com.example.btlandroid.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btlandroid.Adapter.FavoriteAdapter;
import com.example.btlandroid.Domain.FavoriteItem;
import com.example.btlandroid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FavoriteActivity extends AppCompatActivity implements FavoriteAdapter.OnItemRemovedListener {

    private RecyclerView favoriteRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyTextView;
    private ImageButton backBtn;
    private List<FavoriteItem> favoriteItems;
    private FavoriteAdapter adapter;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        favoriteRecyclerView = findViewById(R.id.favoriteRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyTextView = findViewById(R.id.emptyTextView);
        backBtn = findViewById(R.id.backBtn);

        backBtn.setOnClickListener(v -> finish());

        favoriteItems = new ArrayList<>();
        adapter = new FavoriteAdapter(favoriteItems, this);
        favoriteRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        favoriteRecyclerView.setAdapter(adapter);

        setupBottomNavigation();

        // Load favorite items
        loadFavorites();
    }

    private void setupBottomNavigation() {
        View homeNav = findViewById(R.id.homeNav);
        View cartNav = findViewById(R.id.cartNav);
        View favoriteNav = findViewById(R.id.favoriteNav);
        View profileNav = findViewById(R.id.profileNav);
        View searchNav = findViewById(R.id.searchNav);

        homeNav.setOnClickListener(v -> {
            Intent intent = new Intent(FavoriteActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        cartNav.setOnClickListener(v -> {
            Intent intent = new Intent(FavoriteActivity.this, CartActivity.class);
            startActivity(intent);
            finish();
        });

        searchNav.setOnClickListener(v -> {
        });

        profileNav.setOnClickListener(v -> {
            Intent intent = new Intent(FavoriteActivity.this, ProfileActivity.class);
            startActivity(intent);
            finish();
        });

        favoriteNav.setSelected(true);
    }

    private void loadFavorites() {
        if (currentUser == null) {
            showEmptyView();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        
        DatabaseReference favoritesRef = FirebaseDatabase.getInstance().getReference("Favorites")
                .child(currentUser.getUid());
        
        favoritesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                favoriteItems.clear();
                
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    FavoriteItem item = dataSnapshot.getValue(FavoriteItem.class);
                    if (item != null) {
                        favoriteItems.add(item);
                    }
                }
                
                progressBar.setVisibility(View.GONE);
                
                if (favoriteItems.isEmpty()) {
                    showEmptyView();
                } else {
                    favoriteRecyclerView.setVisibility(View.VISIBLE);
                    emptyTextView.setVisibility(View.GONE);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                showEmptyView();
            }
        });
    }

    private void showEmptyView() {
        favoriteRecyclerView.setVisibility(View.GONE);
        emptyTextView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onItemRemoved() {
        if (favoriteItems.isEmpty()) {
            showEmptyView();
        }
    }
}
