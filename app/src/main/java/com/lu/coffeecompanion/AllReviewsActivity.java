package com.lu.coffeecompanion;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.lu.coffeecompanion.adapters.ReviewAdapter;
import com.lu.coffeecompanion.models.Review;

import java.util.ArrayList;
import java.util.List;

public class AllReviewsActivity extends AppCompatActivity {

    private static final String TAG = "AllReviewsActivity";

    private RecyclerView recyclerViewReviews;
    private ReviewAdapter adapter;
    private List<Review> reviewList;
    private FirebaseFirestore db;

    private TextView tvProductName;
    private ImageView ivProductImage;
    private TextView tvNoReviews;
    private ProgressBar progressBar;
    private Button btnBack;

    private String menuItemId;
    private String productName;
    private String imageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_reviews);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Get data from intent
        menuItemId = getIntent().getStringExtra("menuItemId");
        productName = getIntent().getStringExtra("productName");
        imageUrl = getIntent().getStringExtra("imageUrl");

        Log.d(TAG, "Product ID: " + menuItemId + ", Name: " + productName);

        // Initialize views
        initializeViews();

        // Setup UI
        setupUI();

        // Load reviews
        if (menuItemId != null) {
            loadAllReviews();
        } else {
            Toast.makeText(this, "Invalid product", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        try {
            btnBack = findViewById(R.id.back_btn);
            recyclerViewReviews = findViewById(R.id.reviews_recycler);

            // Optional views - don't crash if missing
            tvProductName = null;
            ivProductImage = null;
            tvNoReviews = findViewById(R.id.tvNoReviews);
            progressBar = findViewById(R.id.progressBar);

            reviewList = new ArrayList<>();
            adapter = new ReviewAdapter(this);

            if (recyclerViewReviews != null) {
                recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this));
                recyclerViewReviews.setAdapter(adapter);
            }

            Log.d(TAG, "Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
        }
    }

    private void setupUI() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        Log.d(TAG, "UI setup completed");
    }

    private void loadAllReviews() {
        showLoading(true);

        Log.d(TAG, "Loading all reviews for product: " + menuItemId);

        // Load ALL orderRatings without filter - then filter in Java
        db.collection("orderRatings")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    showLoading(false);

                    Log.d(TAG, "Got all orderRatings. Total count: " + querySnapshot.size());

                    if (querySnapshot.isEmpty()) {
                        Log.d(TAG, "No ratings found in collection");
                        showNoReviews();
                        return;
                    }

                    // Filter and sort in Java instead of Firestore
                    List<DocumentSnapshot> allDocs = new ArrayList<>(querySnapshot.getDocuments());
                    List<DocumentSnapshot> matchedDocs = new ArrayList<>();

                    for (DocumentSnapshot doc : allDocs) {
                        String docProductId = doc.getString("productId");

                        Log.d(TAG, "Checking rating - productId: " + docProductId +
                                ", looking for: " + menuItemId);

                        if (docProductId != null && docProductId.equals(menuItemId)) {
                            matchedDocs.add(doc);
                            Log.d(TAG, "MATCHED: " + doc.getString("userName"));
                        }
                    }

                    Log.d(TAG, "Found " + matchedDocs.size() + " matching ratings");

                    if (matchedDocs.isEmpty()) {
                        Log.d(TAG, "No ratings found for this product");
                        showNoReviews();
                        return;
                    }

                    // Sort by timestamp descending
                    matchedDocs.sort((d1, d2) -> {
                        com.google.firebase.Timestamp t1 = d1.getTimestamp("timestamp");
                        com.google.firebase.Timestamp t2 = d2.getTimestamp("timestamp");
                        if (t1 == null || t2 == null) return 0;
                        return t2.compareTo(t1);
                    });

                    processReviews(matchedDocs);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading reviews: " + e.getMessage(), e);
                    showLoading(false);
                    showNoReviews();
                    Toast.makeText(this, "Failed to load reviews: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void processReviews(List<DocumentSnapshot> matchedDocs) {
        reviewList.clear();
        for (DocumentSnapshot document : matchedDocs) {
            try {
                Review review = new Review();
                review.setReviewId(document.getId());
                review.setOrderId(document.getString("orderId"));
                review.setMenuItemId(document.getString("productId"));
                review.setUserId(document.getString("userId"));
                review.setUserName(document.getString("userName"));

                Double rating = document.getDouble("rating");
                if (rating != null) {
                    review.setRating(rating.floatValue());
                }

                review.setFeedback(document.getString("comment"));
                review.setCreatedAt(document.getTimestamp("timestamp"));

                reviewList.add(review);
                Log.d(TAG, "Added rating: " + review.getUserName() + " - " + review.getRating() + "★");
            } catch (Exception e) {
                Log.e(TAG, "Error parsing rating", e);
            }
        }

        updateUI();
    }

    private void updateUI() {
        try {
            if (reviewList.isEmpty()) {
                showNoReviews();
            } else {
                adapter.setReviewList(reviewList);
                if (recyclerViewReviews != null) {
                    recyclerViewReviews.setVisibility(View.VISIBLE);
                }
                if (tvNoReviews != null) {
                    tvNoReviews.setVisibility(View.GONE);
                }
                Log.d(TAG, "Displayed " + reviewList.size() + " reviews");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI", e);
        }
    }

    private void showNoReviews() {
        try {
            if (recyclerViewReviews != null) {
                recyclerViewReviews.setVisibility(View.GONE);
            }
            if (tvNoReviews != null) {
                tvNoReviews.setVisibility(View.VISIBLE);
                TextView tvMessage = tvNoReviews.findViewById(android.R.id.text1);
                if (tvMessage == null) {
                    tvMessage = new TextView(this);
                }
                tvMessage.setText("No reviews yet for this product");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing no reviews message", e);
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}