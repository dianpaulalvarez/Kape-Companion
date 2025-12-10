package com.lu.coffeecompanion;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lu.coffeecompanion.databinding.FragmentMenuBinding;

import java.util.ArrayList;
import java.util.List;

public class MenuFragment extends Fragment {

    private static final String TAG = "MenuFragment";
    private FragmentMenuBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private int dataSourcesLoaded = 0;
    private final int TOTAL_DATA_SOURCES = 1; // Only inventory
    private boolean isFirstLoad = true; // Add this flag

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMenuBinding.inflate(inflater, container, false);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Log.d(TAG, "MenuFragment onCreateView called");

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            navigateToLogin();
            return binding.getRoot();
        }

        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "Refreshing menu");
            refreshMenu();
        });

        // Initialize UI
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.menuContainer.setVisibility(View.GONE);
        binding.emptyStateContainer.setVisibility(View.GONE);

        if (isFirstLoad) {
            loadAllMenuData();
            isFirstLoad = false;
        }

        return binding.getRoot();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(requireContext(), MainActivity.class);
        startActivity(intent);
        requireActivity().finish();
    }

    private void refreshMenu() {
        binding.menuContainer.removeAllViews();
        dataSourcesLoaded = 0;
        loadAllMenuData();
    }

    private void loadAllMenuData() {
        Log.d(TAG, "Loading ALL menu data...");

        // Reset counter
        dataSourcesLoaded = 0;

        // Load both data sources
        //loadCoffees();
        loadInventoryItems();
    }

    private void loadCoffees() {
        Log.d(TAG, "Attempting to load coffees collection...");

        db.collection("coffees")
                .orderBy("order")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int count = task.getResult().size();
                        Log.d(TAG, "✓ SUCCESS: Loaded " + count + " coffee items");

                        // Add coffee items to UI
                        for (DocumentSnapshot document : task.getResult()) {
                            addCoffeeItemToView(document);
                        }
                    } else {
                        Log.e(TAG, "✗ FAILED to load coffees: " + task.getException().getMessage());
                        Toast.makeText(requireContext(),
                                "Note: Coffee items not available", Toast.LENGTH_SHORT).show();
                    }

                    // Mark this data source as loaded
                    dataSourceLoaded();
                });
    }

    private void loadInventoryItems() {
        Log.d(TAG, "Attempting to load inventory collection...");

        db.collection("inventory")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int totalDocs = task.getResult().size();
                        int displayedItems = 0;

                        Log.d(TAG, "✓ SUCCESS: Found " + totalDocs + " inventory documents");

                        for (DocumentSnapshot document : task.getResult()) {
                            String id = document.getId();
                            String productName = document.getString("productName");
                            Long quantity = document.getLong("quantity");
                            Double price = document.getDouble("price");
                            Boolean isArchived = document.getBoolean("isArchived");
                            String imageUrl = document.getString("imageUrl");

                            Log.d(TAG, "Inventory item: " + productName +
                                    ", Archived: " + isArchived +
                                    ", Price: " + price);

                            // Only add if NOT archived and has stock
                            boolean shouldAdd = (isArchived == null || !isArchived) &&
                                    productName != null &&
                                    quantity != null && quantity > 0 &&
                                    price != null;

                            if (shouldAdd) {
                                addInventoryItemToView(id, productName, quantity.intValue(), price, imageUrl);
                                displayedItems++;
                            }
                        }

                        Log.d(TAG, "✓ Displaying " + displayedItems + " inventory items (filtered)");
                    } else {
                        Log.e(TAG, "✗ FAILED to load inventory: " + task.getException().getMessage());
                        Toast.makeText(requireContext(),
                                "Note: Inventory items not available", Toast.LENGTH_SHORT).show();
                    }

                    // Mark this data source as loaded
                    dataSourceLoaded();
                });
    }

    private void dataSourceLoaded() {
        dataSourcesLoaded++;
        Log.d(TAG, "Data source loaded: " + dataSourcesLoaded + "/" + TOTAL_DATA_SOURCES);

        // When both data sources are loaded, hide progress and show results
        if (dataSourcesLoaded >= TOTAL_DATA_SOURCES) {
            if (binding.swipeRefreshLayout.isRefreshing()) {
                binding.swipeRefreshLayout.setRefreshing(false);
            }

            // Check if we have any items in the container
            if (binding.menuContainer.getChildCount() == 0) {
                showEmptyState();
            } else {
                binding.progressBar.setVisibility(View.GONE);
                binding.menuContainer.setVisibility(View.VISIBLE);
                binding.emptyStateContainer.setVisibility(View.GONE);

                // Show total count
                showTotalCount();
            }
        }
    }

    private void addCoffeeItemToView(DocumentSnapshot document) {
        try {
            String coffeeName = document.getString("name");
            String imageUrl = document.getString("imageUrl");
            String docId = document.getId();
            String description = document.getString("description");

            if (coffeeName == null || coffeeName.isEmpty()) {
                Log.w(TAG, "Skipping coffee item with no name");
                return;
            }

            View coffeeLayout = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_coffee_vertical_simple, binding.menuContainer, false);

            ImageView coffeeImage = coffeeLayout.findViewById(R.id.coffee_image);
            TextView coffeeText = coffeeLayout.findViewById(R.id.coffee_name);
            TextView coffeeDescription = coffeeLayout.findViewById(R.id.coffee_description);

            coffeeText.setText(coffeeName);

            if (description != null && !description.isEmpty()) {
                coffeeDescription.setText(description);
            } else {
                coffeeDescription.setVisibility(View.GONE);
            }

            // Load image with Glide
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(requireContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.image_placeholder)
                        .into(coffeeImage);
            }

            // Set click listener for coffee items
            coffeeLayout.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), CoffeePicksActivity.class);
                intent.putExtra("docId", docId);
                startActivity(intent);
            });

            binding.menuContainer.addView(coffeeLayout);
            Log.d(TAG, "✓ Added coffee: " + coffeeName);

        } catch (Exception e) {
            Log.e(TAG, "Error adding coffee item: " + e.getMessage());
        }
    }

    private void addInventoryItemToView(String id, String productName, int quantity, double price, String imageUrl) {
        try {
            View inventoryLayout = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_coffee_vertical_simple, binding.menuContainer, false);

            ImageView itemImage = inventoryLayout.findViewById(R.id.coffee_image);
            TextView itemName = inventoryLayout.findViewById(R.id.coffee_name);
            TextView itemDescription = inventoryLayout.findViewById(R.id.coffee_description);

            // Set item details
            itemName.setText(productName);

            // Show price and stock in description
            String descriptionText = String.format("₱%.2f", price);
            itemDescription.setText(descriptionText);
            itemDescription.setVisibility(View.VISIBLE);

            // Load image with Glide
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(requireContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.image_placeholder)
                        .into(itemImage);
            }

            // Set click listener for inventory items
            inventoryLayout.setOnClickListener(v -> {
                Toast.makeText(requireContext(),
                        productName + " - ₱" + String.format("%.2f", price),
                        Toast.LENGTH_SHORT).show();
            });

            binding.menuContainer.addView(inventoryLayout);
            Log.d(TAG, "✓ Added inventory: " + productName + " (₱" + price + ")");

        } catch (Exception e) {
            Log.e(TAG, "Error adding inventory item: " + e.getMessage());
        }
    }

    private void showTotalCount() {
        int itemCount = binding.menuContainer.getChildCount();
        TextView countView = new TextView(requireContext());
        countView.setText("Total Items: " + itemCount);
        countView.setTextSize(12);
        countView.setTextColor(getResources().getColor(android.R.color.darker_gray));
        countView.setPadding(16, 16, 16, 32);
        countView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        binding.menuContainer.addView(countView);

        Log.d(TAG, "Total items displayed: " + itemCount);
    }

    private void showEmptyState() {
        binding.progressBar.setVisibility(View.GONE);
        binding.menuContainer.setVisibility(View.GONE);
        binding.emptyStateContainer.setVisibility(View.VISIBLE);
        Log.d(TAG, "Showing empty state - no items found");
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        isFirstLoad = true;
    }
}