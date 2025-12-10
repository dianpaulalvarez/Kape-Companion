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
    private List<DocumentSnapshot> allMenuItems = new ArrayList<>();

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
            binding.swipeRefreshLayout.setRefreshing(false);
        });

        // Initialize progress bar
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.menuContainer.setVisibility(View.GONE);
        binding.emptyStateContainer.setVisibility(View.GONE);

        loadAllMenuItems();

        return binding.getRoot();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(requireContext(), MainActivity.class);
        startActivity(intent);
        requireActivity().finish();
    }

    private void refreshMenu() {
        binding.menuContainer.removeAllViews();
        allMenuItems.clear();
        loadAllMenuItems();
    }

    private void loadAllMenuItems() {
        Log.d(TAG, "Loading ALL menu items...");

        // NO LIMIT - GET ALL ITEMS
        db.collection("coffees")
                .orderBy("order")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Successfully loaded " + queryDocumentSnapshots.size() + " menu items");

                    allMenuItems.clear();
                    allMenuItems.addAll(queryDocumentSnapshots.getDocuments());

                    if (allMenuItems.isEmpty()) {
                        showEmptyState();
                        return;
                    }

                    showMenuItems();
                    binding.progressBar.setVisibility(View.GONE);
                    binding.menuContainer.setVisibility(View.VISIBLE);
                    binding.emptyStateContainer.setVisibility(View.GONE);

                    Log.d(TAG, "Displaying " + allMenuItems.size() + " items");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load menu: " + e.getMessage());
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to load menu", Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
    }

    private void showMenuItems() {
        binding.menuContainer.removeAllViews();

        for (DocumentSnapshot document : allMenuItems) {
            addMenuItemToView(document);
        }

        // Show item count
        TextView itemCount = new TextView(requireContext());
        itemCount.setText("Total Items: " + allMenuItems.size());
        itemCount.setTextSize(12);
        itemCount.setTextColor(getResources().getColor(android.R.color.darker_gray));
        itemCount.setPadding(16, 16, 16, 32);
        itemCount.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        binding.menuContainer.addView(itemCount);
    }

    private void addMenuItemToView(DocumentSnapshot document) {
        String coffeeName = document.getString("name");
        String imageUrl = document.getString("imageUrl");
        String docId = document.getId();
        String description = document.getString("description");
        String category = document.getString("category");

        Log.d(TAG, "Adding menu item: " + coffeeName);

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

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(requireContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.image_placeholder)
                    .into(coffeeImage);
        }

        coffeeLayout.setOnClickListener(v -> {
            Log.d(TAG, "Menu item clicked: " + coffeeName);
            Intent intent = new Intent(requireContext(), CoffeePicksActivity.class);
            intent.putExtra("docId", docId);
            startActivity(intent);
        });

        binding.menuContainer.addView(coffeeLayout);
    }

    private void showEmptyState() {
        binding.progressBar.setVisibility(View.GONE);
        binding.menuContainer.setVisibility(View.GONE);
        binding.emptyStateContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "MenuFragment destroyed");
        binding = null;
    }
}