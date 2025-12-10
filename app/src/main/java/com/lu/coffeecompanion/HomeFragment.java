package com.lu.coffeecompanion;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.lu.coffeecompanion.databinding.HomeFragmentBinding;

import java.util.List;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    HomeFragmentBinding binding;
    FirebaseFirestore db;
    FirebaseAuth auth;
    FirebaseUser currentUser;
    String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = HomeFragmentBinding.inflate(inflater, container, false);
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        if (currentUser == null) {
            Intent intent = new Intent(requireContext(), MainActivity.class);
            startActivity(intent);
            requireActivity().finish();
            return binding.getRoot();
        }

        binding.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                binding.itemContainer1.removeAllViews();
                fetchBestSellers();
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        });

        binding.cart.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CartActivity.class);
            startActivity(intent);
        });

        fetchBestSellers();
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void fetchBestSellers() {
        // Query for best sellers OR fallback to regular items
        db.collection("coffees")
                .whereEqualTo("bestSeller", true)
                .orderBy("order")
                .limit(8)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // Fallback: get regular items
                        fetchRegularItems();
                        return;
                    }

                    displayItems(queryDocumentSnapshots.getDocuments(), true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to load items", Toast.LENGTH_SHORT).show();
                    fetchRegularItems();
                });
    }

    private void fetchRegularItems() {
        db.collection("coffees")
                .orderBy("order")
                .limit(6)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    displayItems(queryDocumentSnapshots.getDocuments(), false);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to load items", Toast.LENGTH_SHORT).show();
                });
    }

    private void displayItems(List<DocumentSnapshot> items, boolean isBestSeller) {
        binding.itemContainer1.removeAllViews();

        for (DocumentSnapshot document : items) {
            String coffeeName = document.getString("name");
            String imageUrl = document.getString("imageUrl");
            String docId = document.getId();
            String description = document.getString("description");

            View coffeeLayout = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_coffee_vertical_simple, binding.itemContainer1, false);

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
                Intent intent = new Intent(requireContext(), CoffeePicksActivity.class);
                intent.putExtra("docId", docId);
                startActivity(intent);
            });

            binding.itemContainer1.addView(coffeeLayout);
        }
    }
}