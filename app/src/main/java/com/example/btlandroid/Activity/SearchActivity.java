package com.example.btlandroid.Activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.btlandroid.Adapter.BestdealAdapter;
import com.example.btlandroid.Domain.ItemDomain;
import com.example.btlandroid.R;
import com.example.btlandroid.Utils.BottomNavigationUtils;
import com.example.btlandroid.databinding.ActivitySearchBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class SearchActivity extends AppCompatActivity {
    private ActivitySearchBinding binding;
    private BestdealAdapter adapter;
    private ArrayList<ItemDomain> itemList = new ArrayList<>();
    private ArrayList<ItemDomain> filteredList = new ArrayList<>();
    
    private String[] categories = {"Tất cả", "Rau", "Hoa quả", "Sữa", "Đồ uống", "Hạt"};
    private String[] sortOptions = {"Mặc định", "Giá tăng dần", "Giá giảm dần", "Đánh giá cao nhất"};
    private String currentCategory = "Tất cả";
    private String currentSort = "Mặc định";
    private double minPrice = 0;
    private double maxPrice = Double.MAX_VALUE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupViews();
        loadAllItems();
    }

    private void setupViews() {
        // Setup RecyclerView
        adapter = new BestdealAdapter(filteredList);
        binding.searchRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        binding.searchRecyclerView.setAdapter(adapter);

        // Setup bottom navigation
        BottomNavigationUtils.setupBottomNavigation(this, R.id.searchNav);

        // Setup spinners
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.categorySpinner.setAdapter(categoryAdapter);

        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, sortOptions);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.sortSpinner.setAdapter(sortAdapter);

        // Setup listeners
        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filterItems();
            }
        });

        binding.categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategory = categories[position];
                filterItems();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSort = sortOptions[position];
                filterItems();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Price range filter
        binding.applyFilterBtn.setOnClickListener(v -> {
            try {
                String minPriceStr = binding.minPriceEditText.getText().toString();
                String maxPriceStr = binding.maxPriceEditText.getText().toString();
                
                minPrice = minPriceStr.isEmpty() ? 0 : Double.parseDouble(minPriceStr);
                maxPrice = maxPriceStr.isEmpty() ? Double.MAX_VALUE : Double.parseDouble(maxPriceStr);
                
                filterItems();
            } catch (NumberFormatException e) {
                binding.minPriceEditText.setError("Giá không hợp lệ");
            }
        });

        // Reset button
        binding.resetFilterBtn.setOnClickListener(v -> {
            binding.searchEditText.setText("");
            binding.categorySpinner.setSelection(0);
            binding.sortSpinner.setSelection(0);
            binding.minPriceEditText.setText("");
            binding.maxPriceEditText.setText("");
            minPrice = 0;
            maxPrice = Double.MAX_VALUE;
            filterItems();
        });
    }

    private void loadAllItems() {
        DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("Items");
        
        binding.progressBar.setVisibility(View.VISIBLE);
        itemsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                itemList.clear();
                
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    ItemDomain item = dataSnapshot.getValue(ItemDomain.class);
                    if (item != null) {
                        itemList.add(item);
                    }
                }
                
                binding.progressBar.setVisibility(View.GONE);
                filterItems();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void filterItems() {
        filteredList.clear();
        String searchText = binding.searchEditText.getText().toString().toLowerCase();
        
        // Apply filters
        for (ItemDomain item : itemList) {
            if (matchesFilters(item, searchText)) {
                filteredList.add(item);
            }
        }
        
        // Apply sorting
        sortFilteredList();
        
        // Update UI
        if (filteredList.isEmpty()) {
            binding.noResultsText.setVisibility(View.VISIBLE);
            binding.searchRecyclerView.setVisibility(View.GONE);
        } else {
            binding.noResultsText.setVisibility(View.GONE);
            binding.searchRecyclerView.setVisibility(View.VISIBLE);
        }
        
        adapter.notifyDataSetChanged();
    }

    private boolean matchesFilters(ItemDomain item, String searchText) {
        if (item == null) return false;
        
        // Text search
        boolean matchesSearch = item.getTitle() != null && item.getTitle().toLowerCase().contains(searchText) ||
                              item.getDescription() != null && item.getDescription().toLowerCase().contains(searchText);
        
        // Category filter with null check
        boolean matchesCategory = currentCategory.equals("Tất cả") || 
                                (item.getCategory() != null && item.getCategory().equals(currentCategory));
        
        // Price range filter
        boolean matchesPrice = item.getPrice() >= minPrice && item.getPrice() <= maxPrice;
        
        return matchesSearch && matchesCategory && matchesPrice;
    }

    private void sortFilteredList() {
        switch (currentSort) {
            case "Giá tăng dần":
                Collections.sort(filteredList, 
                    (item1, item2) -> Double.compare(item1.getPrice(), item2.getPrice()));
                break;
            case "Giá giảm dần":
                Collections.sort(filteredList, 
                    (item1, item2) -> Double.compare(item2.getPrice(), item1.getPrice()));
                break;
            case "Đánh giá cao nhất":
                Collections.sort(filteredList, 
                    (item1, item2) -> Double.compare(item2.getStar(), item1.getStar()));
                break;
        }
    }
}
