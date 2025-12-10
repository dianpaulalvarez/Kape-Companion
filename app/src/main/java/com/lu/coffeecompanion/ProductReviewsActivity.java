package com.lu.coffeecompanion;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.lu.coffeecompanion.adapters.ProductReviewAdapter;
import com.lu.coffeecompanion.models.ProductReview;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProductReviewsActivity extends AppCompatActivity {

    private static final String TAG = "PRODUCT_REVIEWS_DEBUG";

    private FirebaseFirestore db;

    private String productId;
    private String productName;

    private ProductReviewAdapter reviewAdapter;
    private List<ProductReview> reviewList = new ArrayList<>();

    // UI Components
    private ImageView btnBack;
    private TextView tvProductName;
    private TextView tvReviewCount;
    private RecyclerView recyclerReviews;
    private TextView tvNoReviews;
    private TextView tvAverageRating;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View loadingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_reviews);

        Log.d(TAG, "=== PRODUCT REVIEWS ACTIVITY STARTED ===");

        db = FirebaseFirestore.getInstance();

        // Get product info from intent
        productId = getIntent().getStringExtra("productId");
        productName = getIntent().getStringExtra("productName");

        Log.d(TAG, "📌 Product ID: " + productId);
        Log.d(TAG, "📌 Product Name: " + productName);

        if (productId == null || productId.isEmpty()) {
            Log.e(TAG, "❌ No product ID provided!");
            Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupUI();
        loadAllReviews();
        loadProductStatistics();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        tvProductName = findViewById(R.id.tvProductName);
        tvReviewCount = findViewById(R.id.tvReviewCount);
        recyclerReviews = findViewById(R.id.recyclerReviews);
        tvNoReviews = findViewById(R.id.tvNoReviews);
        tvAverageRating = findViewById(R.id.tvAverageRating);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        loadingView = findViewById(R.id.loadingView);

        Log.d(TAG, "✅ UI views initialized");
    }

    private void setupUI() {
        // Set product name
        if (productName != null && !productName.isEmpty()) {
            tvProductName.setText(productName);
        } else {
            tvProductName.setText("Product Reviews");
        }

        // Back button
        btnBack.setOnClickListener(v -> {
            Log.d(TAG, "← Going back");
            finish();
        });

        // Setup RecyclerView
        recyclerReviews.setLayoutManager(new LinearLayoutManager(this));
        reviewAdapter = new ProductReviewAdapter(reviewList);
        recyclerReviews.setAdapter(reviewAdapter);

        // Setup Swipe to Refresh
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                Log.d(TAG, "🔁 Swipe to refresh triggered");
                refreshAllData();
            });
        }

        // Set loading state
        showLoading(true);
    }

    private void loadAllReviews() {
        Log.d(TAG, "🔄 Loading all reviews for product: " + productId);

        db.collection("productRatings")
                .whereEqualTo("productId", productId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "✅ Reviews query completed");

                    if (queryDocumentSnapshots == null) {
                        Log.e(TAG, "❌ Query snapshot is null");
                        handleNoReviews();
                        return;
                    }

                    reviewList.clear();

                    if (!queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "📊 Found " + queryDocumentSnapshots.size() + " reviews");

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            try {
                                Log.d(TAG, "📄 Processing review document: " + document.getId());

                                ProductReview review = new ProductReview();
                                review.setId(document.getId());
                                review.setProductId(document.getString("productId"));
                                review.setUserId(document.getString("userId"));
                                review.setUserName(document.getString("userName"));
                                review.setOrderId(document.getString("orderId"));

                                // Handle rating
                                Object ratingObj = document.get("rating");
                                if (ratingObj != null) {
                                    if (ratingObj instanceof Double) {
                                        review.setRating(((Double) ratingObj).floatValue());
                                    } else if (ratingObj instanceof Long) {
                                        review.setRating(((Long) ratingObj).floatValue());
                                    } else if (ratingObj instanceof Integer) {
                                        review.setRating(((Integer) ratingObj).floatValue());
                                    }
                                }

                                review.setComment(document.getString("comment"));

                                // Handle timestamp
                                Timestamp timestamp = document.getTimestamp("timestamp");
                                if (timestamp != null) {
                                    Date date = timestamp.toDate();
                                    review.setTimestamp(date);
                                    Log.d(TAG, "📅 Review date: " + date.toString());
                                }

                                reviewList.add(review);
                                Log.d(TAG, "✅ Added review from: " + review.getUserName());

                            } catch (Exception e) {
                                Log.e(TAG, "❌ Error parsing review document", e);
                            }
                        }

                        updateUIAfterLoading();
                    } else {
                        Log.d(TAG, "⚠️ No reviews found for this product");
                        handleNoReviews();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error loading reviews: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to load reviews: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    handleNoReviews();
                });
    }

    private void loadProductStatistics() {
        Log.d(TAG, "📊 Loading product statistics for: " + productId);

        db.collection("products")
                .document(productId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Double averageRating = documentSnapshot.getDouble("averageRating");
                        Long ratingCount = documentSnapshot.getLong("ratingCount");

                        if (averageRating != null && tvAverageRating != null) {
                            tvAverageRating.setText(String.format(Locale.getDefault(), "%.1f", averageRating));
                            Log.d(TAG, "⭐ Average rating: " + averageRating);
                        }

                        if (ratingCount != null && tvReviewCount != null) {
                            String reviewText = ratingCount + (ratingCount == 1 ? " review" : " reviews");
                            tvReviewCount.setText(reviewText);
                            Log.d(TAG, "📈 Review count from products collection: " + ratingCount);
                        }
                    } else {
                        Log.w(TAG, "⚠️ Product statistics not found in 'products' collection");

                        // Fallback: Calculate from productRatings collection
                        calculateStatsFromReviews();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error loading product statistics", e);
                    calculateStatsFromReviews();
                });
    }

    private void calculateStatsFromReviews() {
        Log.d(TAG, "🔢 Calculating stats from reviews collection");

        db.collection("productRatings")
                .whereEqualTo("productId", productId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int count = snapshot.size();
                    float totalRating = 0;

                    for (QueryDocumentSnapshot doc : snapshot) {
                        Double rating = doc.getDouble("rating");
                        if (rating != null) {
                            totalRating += rating;
                        }
                    }

                    float avg = count > 0 ? totalRating / count : 0;

                    if (tvAverageRating != null) {
                        tvAverageRating.setText(String.format(Locale.getDefault(), "%.1f", avg));
                        Log.d(TAG, "📊 Calculated average: " + avg);
                    }

                    if (tvReviewCount != null) {
                        String reviewText = count + (count == 1 ? " review" : " reviews");
                        tvReviewCount.setText(reviewText);
                        Log.d(TAG, "📊 Calculated count: " + count);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error calculating stats from reviews", e);
                });
    }

    private void updateUIAfterLoading() {
        showLoading(false);

        if (reviewList.isEmpty()) {
            handleNoReviews();
            return;
        }

        reviewAdapter.notifyDataSetChanged();

        // Update review count based on loaded reviews
        if (tvReviewCount != null) {
            String reviewText = reviewList.size() + (reviewList.size() == 1 ? " review" : " reviews");
            tvReviewCount.setText(reviewText);
            Log.d(TAG, "📱 Displaying " + reviewList.size() + " reviews");
        }

        // Show/hide views
        tvNoReviews.setVisibility(View.GONE);
        recyclerReviews.setVisibility(View.VISIBLE);

        // Stop refresh animation
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
            Log.d(TAG, "🔄 Refresh completed");
        }
    }

    private void handleNoReviews() {
        showLoading(false);
        reviewAdapter.notifyDataSetChanged();
        tvNoReviews.setVisibility(View.VISIBLE);
        recyclerReviews.setVisibility(View.GONE);

        if (tvReviewCount != null) {
            tvReviewCount.setText("0 reviews");
        }

        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }

        Log.d(TAG, "📱 Showing 'No reviews' message");
    }

    private void showLoading(boolean isLoading) {
        if (loadingView != null) {
            loadingView.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }

        if (isLoading) {
            recyclerReviews.setVisibility(View.GONE);
            tvNoReviews.setVisibility(View.GONE);
        }
    }

    private void refreshAllData() {
        Log.d(TAG, "🔄 Refreshing all data");
        loadAllReviews();
        loadProductStatistics();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "🔄 Activity resumed - refreshing data");

        // Refresh data when returning to activity
        if (productId != null) {
            refreshAllData();
        }
    }
}