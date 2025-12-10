package com.lu.coffeecompanion;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.lu.coffeecompanion.adapters.ToReviewAdapter;
import com.lu.coffeecompanion.databinding.ActivityToReviewBinding;
import com.lu.coffeecompanion.models.OrderModel;

import java.util.ArrayList;
import java.util.List;

public class ToReviewActivity extends AppCompatActivity {

    private static final String TAG = "ToReviewActivity";

    private ActivityToReviewBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private String userId;

    private ToReviewAdapter adapter;
    private List<OrderModel> orderList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityToReviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeFirebase();
        setupToolbar();
        setupRecyclerView();
        loadToReviewOrders();
    }

    private void initializeFirebase() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Orders to Review");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new ToReviewAdapter(orderList, this::onOrderClicked);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }

    private void loadToReviewOrders() {
        if (userId == null) {
            showEmptyState("Please login to view orders");
            return;
        }

        Log.d(TAG, "Loading orders for userId: " + userId);
        showLoading(true);

        // Simple query - load all orders for user, then filter in code
        firestore.collection("orders")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    orderList.clear();

                    Log.d(TAG, "Total orders found: " + queryDocumentSnapshots.size());

                    if (queryDocumentSnapshots.isEmpty()) {
                        showEmptyState("No orders to review");
                        return;
                    }

                    // Filter in code to avoid index requirement
                    int filteredCount = 0;
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            String status = document.getString("status");
                            Boolean reviewed = document.getBoolean("reviewed");

                            Log.d(TAG, "Order: " + document.getId() + " | Status: " + status + " | Reviewed: " + reviewed);

                            // Check if matches criteria: Completed status and not reviewed
                            if (status != null && status.equalsIgnoreCase("Completed") &&
                                    reviewed != null && !reviewed) {

                                OrderModel order = document.toObject(OrderModel.class);
                                order.setOrderId(document.getId());
                                orderList.add(order);
                                filteredCount++;
                                Log.d(TAG, "✓ Added order for review: " + order.getOrderId());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing order: " + e.getMessage());
                        }
                    }

                    Log.d(TAG, "Total filtered orders: " + filteredCount);

                    if (orderList.isEmpty()) {
                        showEmptyState("No orders to review");
                    } else {
                        adapter.notifyDataSetChanged();
                        binding.emptyState.setVisibility(View.GONE);
                        binding.recyclerView.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading orders", e);
                    showLoading(false);
                    showEmptyState("Failed to load orders: " + e.getMessage());
                });
    }

    private void onOrderClicked(OrderModel order) {
        Intent intent = new Intent(this, ReviewActivity.class);
        intent.putExtra("orderId", order.getOrderId());
        startActivity(intent);
    }

    private void showLoading(boolean show) {
        if (show) {
            binding.recyclerView.setVisibility(View.GONE);
            binding.emptyState.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(String message) {
        binding.recyclerView.setVisibility(View.GONE);
        binding.emptyState.setVisibility(View.VISIBLE);

        TextView tvMessage = binding.emptyState.findViewById(R.id.tvMessage);
        if (tvMessage != null) {
            tvMessage.setText(message);
        }
    }
}