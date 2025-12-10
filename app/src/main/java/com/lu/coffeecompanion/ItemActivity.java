package com.lu.coffeecompanion;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.lu.coffeecompanion.adapters.ProductReviewAdapter;
import com.lu.coffeecompanion.models.ProductReview;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ItemActivity extends AppCompatActivity {

    private static final String TAG = "ItemActivity";

    // UI Components
    private ImageView back, itemImage;
    private TextView itemTitle, itemPrice, itemDescription;
    private TextView tvAverageRating, tvReviewCountDisplay, tvNoReviews;
    private RatingBar avgRatingBar;
    private Button btnSeeAllReviews, btnMinus, btnAdd, addToCart, buyNow;
    private TextView quantity;
    private RecyclerView recyclerReviews;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String userId;

    // Data
    private int counter = 1;
    private String shopDocumentId;
    private String menuDocId;
    private String productName;
    private double productPrice;
    private String productDescriptionStr;
    private String imageUrl;
    private String category;

    // Adapter
    private ProductReviewAdapter reviewAdapter;
    private List<ProductReview> reviewList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item);

        Log.d(TAG, "ItemActivity onCreate");

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
            return;
        } else {
            userId = currentUser.getUid();
            Log.d(TAG, "User ID: " + userId);
        }

        // Get intent data
        Intent getIntent = getIntent();
        shopDocumentId = getIntent.getStringExtra("documentId");
        menuDocId = getIntent.getStringExtra("docId");

        Log.d(TAG, "Shop ID: " + shopDocumentId + ", Menu ID: " + menuDocId);

        if (shopDocumentId == null || menuDocId == null) {
            Toast.makeText(this, "Invalid product data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            // Initialize views
            initializeViews();

            // Setup UI
            setupUI();

            // Load data
            loadItemDetails();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading product", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeViews() {
        try {
            // Toolbar
            back = findViewById(R.id.back);

            // Product info
            itemImage = findViewById(R.id.itemImage);
            itemTitle = findViewById(R.id.itemTitle);
            itemPrice = findViewById(R.id.itemPrice);
            itemDescription = findViewById(R.id.itemDescription);

            // Rating section
            avgRatingBar = findViewById(R.id.avgRatingBar);
            tvAverageRating = findViewById(R.id.tvAverageRating);
            tvReviewCountDisplay = findViewById(R.id.tvReviewCountDisplay);

            // Check if btnSeeAllReviews exists
            try {
                btnSeeAllReviews = findViewById(R.id.btnSeeAllReviews);
            } catch (Exception e) {
                Log.w(TAG, "btnSeeAllReviews not found in layout");
                btnSeeAllReviews = null;
            }

            // Quantity
            btnMinus = findViewById(R.id.btnMinus);
            btnAdd = findViewById(R.id.btnAdd);
            quantity = findViewById(R.id.quantity);

            // Action buttons
            addToCart = findViewById(R.id.addToCart);
            buyNow = findViewById(R.id.buyNow);

            // Reviews
            recyclerReviews = findViewById(R.id.recyclerReviews);
            tvNoReviews = findViewById(R.id.tvNoReviews);

            // Loading - check if exists
            try {
                progressBar = findViewById(R.id.progressBar);
            } catch (Exception e) {
                Log.w(TAG, "progressBar not found in layout");
                progressBar = null;
            }

            Log.d(TAG, "All views initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
            Toast.makeText(this, "UI Error", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupUI() {
        try {
            // Back button
            back.setOnClickListener(v -> finish());

            // Quantity buttons
            btnAdd.setOnClickListener(v -> {
                try {
                    counter = Integer.parseInt(quantity.getText().toString().trim());
                    counter++;
                    quantity.setText(String.valueOf(counter));
                } catch (NumberFormatException e) {
                    quantity.setText("1");
                    counter = 1;
                }
            });

            btnMinus.setOnClickListener(v -> {
                try {
                    counter = Integer.parseInt(quantity.getText().toString().trim());
                    if (counter > 1) counter--;
                    quantity.setText(String.valueOf(counter));
                } catch (NumberFormatException e) {
                    quantity.setText("1");
                    counter = 1;
                }
            });

            // Action buttons
            addToCart.setOnClickListener(v -> addToCart());
            buyNow.setOnClickListener(v -> buyNow());

            // See All Reviews button - UPDATED
            if (btnSeeAllReviews != null) {
                btnSeeAllReviews.setOnClickListener(v -> openAllReviews());
            }

            // Setup RecyclerView for reviews
            recyclerReviews.setLayoutManager(new LinearLayoutManager(this));
            reviewAdapter = new ProductReviewAdapter(reviewList);
            recyclerReviews.setAdapter(reviewAdapter);

            Log.d(TAG, "UI setup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error in setupUI: " + e.getMessage(), e);
        }
    }

    private void loadItemDetails() {
        Log.d(TAG, "Loading item details...");
        showLoading(true);

        db.collection("shops")
                .document(shopDocumentId)
                .collection("menu")
                .document(menuDocId)
                .get()
                .addOnCompleteListener(task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc.exists()) {
                            productName = doc.getString("name");
                            productPrice = doc.getDouble("price") != null ? doc.getDouble("price") : 0.0;
                            productDescriptionStr = doc.getString("description");
                            imageUrl = doc.getString("imageUrl");
                            category = doc.getString("category");

                            Log.d(TAG, "Product loaded: " + productName + ", Price: " + productPrice);

                            // Update UI
                            updateProductUI();

                            // Load rating stats and reviews
                            loadProductRatingStats();
                            loadReviews();
                        } else {
                            Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Log.e(TAG, "Failed to load product details", task.getException());
                        Toast.makeText(this, "Failed to load product", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void updateProductUI() {
        try {
            // Set product info
            if (itemTitle != null) {
                itemTitle.setText(productName != null ? productName : "Product Name");
            }

            if (itemPrice != null) {
                itemPrice.setText(String.format(Locale.getDefault(), "₱%.2f", productPrice));
            }

            if (itemDescription != null) {
                itemDescription.setText(productDescriptionStr != null ? productDescriptionStr : "No description available");
            }

            // Load product image
            if (itemImage != null && imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image)
                        .into(itemImage);
            }

            Log.d(TAG, "Product UI updated");

        } catch (Exception e) {
            Log.e(TAG, "Error updating product UI: " + e.getMessage(), e);
        }
    }

    // UPDATED: Load from reviews collection instead of productRatings
    private void loadProductRatingStats() {
        Log.d(TAG, "Loading product rating stats...");

        // Query reviews collection for this product
        db.collection("reviews")
                .whereEqualTo("menuItemId", menuDocId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        calculateAndUpdateRating(querySnapshot);
                    } else {
                        Log.d(TAG, "No reviews yet for this product");
                        showDefaultRating();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load product stats", e);
                    showDefaultRating();
                });
    }

    // UPDATED: Calculate rating from reviews
    private void calculateAndUpdateRating(com.google.firebase.firestore.QuerySnapshot querySnapshot) {
        try {
            float totalRating = 0;
            int ratingCount = querySnapshot.size();

            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Double rating = doc.getDouble("rating");
                if (rating != null) {
                    totalRating += rating;
                }
            }

            float averageRating = ratingCount > 0 ? totalRating / ratingCount : 0;

            if (avgRatingBar != null) {
                avgRatingBar.setRating(averageRating);
                Log.d(TAG, "Average rating: " + averageRating);
            }

            if (tvAverageRating != null) {
                tvAverageRating.setText(String.format(Locale.getDefault(), "%.1f", averageRating));
            }

            if (tvReviewCountDisplay != null) {
                if (ratingCount > 0) {
                    tvReviewCountDisplay.setText(String.format(Locale.getDefault(), "(%d reviews)", ratingCount));
                    Log.d(TAG, "Found " + ratingCount + " reviews");
                } else {
                    tvReviewCountDisplay.setText("(No reviews yet)");
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error calculating rating: " + e.getMessage(), e);
        }
    }

    private void showDefaultRating() {
        try {
            if (avgRatingBar != null) {
                avgRatingBar.setRating(0);
            }
            if (tvAverageRating != null) {
                tvAverageRating.setText("0.0");
            }
            if (tvReviewCountDisplay != null) {
                tvReviewCountDisplay.setText("(No reviews yet)");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing default rating: " + e.getMessage(), e);
        }
    }

    // UPDATED: Load reviews from reviews collection
    private void loadReviews() {
        Log.d(TAG, "Loading reviews for product: " + menuDocId);

        db.collection("reviews")
                .whereEqualTo("menuItemId", menuDocId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    reviewList.clear();

                    if (!queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " reviews");

                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            try {
                                ProductReview review = new ProductReview();
                                review.setId(document.getId());
                                review.setProductId(document.getString("menuItemId"));
                                review.setUserId(document.getString("userId"));
                                review.setUserName(document.getString("userName"));
                                review.setOrderId(document.getString("orderId"));

                                Double rating = document.getDouble("rating");
                                if (rating != null) {
                                    review.setRating(rating.floatValue());
                                }

                                // Get feedback field
                                String comment = document.getString("feedback");
                                if (comment == null) {
                                    comment = document.getString("comment");
                                }
                                review.setComment(comment);

                                if (document.getTimestamp("createdAt") != null) {
                                    review.setTimestamp(document.getTimestamp("createdAt").toDate());
                                }

                                reviewList.add(review);
                                Log.d(TAG, "Added review from: " + document.getString("userName"));
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing review document", e);
                            }
                        }

                        updateReviewsUI();
                    } else {
                        Log.d(TAG, "No reviews found for this product");
                        showNoReviews();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading reviews: " + e.getMessage());
                    showNoReviews();
                });
    }

    private void updateReviewsUI() {
        // Hide recent reviews section - all reviews shown in separate page
        try {
            if (recyclerReviews != null) recyclerReviews.setVisibility(View.GONE);
            if (tvNoReviews != null) tvNoReviews.setVisibility(View.GONE);
            Log.d(TAG, "Recent reviews section hidden - use See All Reviews button");
        } catch (Exception e) {
            Log.e(TAG, "Error updating reviews UI: " + e.getMessage(), e);
        }
    }

    private void showNoReviews() {
        // Hide recent reviews section
        try {
            if (recyclerReviews != null) recyclerReviews.setVisibility(View.GONE);
            if (tvNoReviews != null) tvNoReviews.setVisibility(View.GONE);
            Log.d(TAG, "No reviews found - use See All Reviews button");
        } catch (Exception e) {
            Log.e(TAG, "Error in showNoReviews: " + e.getMessage(), e);
        }
    }

    // UPDATED: Open all reviews with product details
    private void openAllReviews() {
        try {
            Intent intent = new Intent(ItemActivity.this, AllReviewsActivity.class);
            intent.putExtra("menuItemId", menuDocId);
            intent.putExtra("productName", productName);
            intent.putExtra("imageUrl", imageUrl);
            startActivity(intent);
            Log.d(TAG, "Opening all reviews for product: " + productName);
        } catch (Exception e) {
            Log.e(TAG, "Error opening reviews: " + e.getMessage(), e);
            Toast.makeText(this, "Cannot open reviews", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoading(boolean isLoading) {
        try {
            if (progressBar != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in showLoading: " + e.getMessage(), e);
        }
    }

    private void addToCart() {
        try {
            int qty = Integer.parseInt(quantity.getText().toString().trim());

            if (qty <= 0) {
                Toast.makeText(this, "Please select at least 1 item.", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> cartItem = new HashMap<>();
            cartItem.put("docId", menuDocId);
            cartItem.put("quantity", qty);
            cartItem.put("shopId", shopDocumentId);
            cartItem.put("itemName", productName);
            cartItem.put("itemPrice", productPrice);
            if (imageUrl != null) {
                cartItem.put("imageUrl", imageUrl);
            }

            // Check if item already in cart
            db.collection("users")
                    .document(userId)
                    .collection("cart")
                    .whereEqualTo("docId", menuDocId)
                    .whereEqualTo("shopId", shopDocumentId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            // Update existing cart item
                            String cartId = task.getResult().getDocuments().get(0).getId();
                            db.collection("users")
                                    .document(userId)
                                    .collection("cart")
                                    .document(cartId)
                                    .update("quantity", FieldValue.increment(qty))
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Item quantity updated in cart", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Failed to update cart", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            // Add new cart item
                            db.collection("users")
                                    .document(userId)
                                    .collection("cart")
                                    .add(cartItem)
                                    .addOnSuccessListener(documentReference -> {
                                        Toast.makeText(this, "Item added to cart", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Failed to add to cart", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error in addToCart: " + e.getMessage(), e);
            Toast.makeText(this, "Error adding to cart", Toast.LENGTH_SHORT).show();
        }
    }

    private void buyNow() {
        try {
            int qty = Integer.parseInt(quantity.getText().toString().trim());

            if (qty <= 0) {
                Toast.makeText(this, "Please select at least 1 item.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent i = new Intent(ItemActivity.this, AddressListActivity.class);
            i.putExtra("orderType", "buy_now");
            i.putExtra("shopId", shopDocumentId);
            i.putExtra("itemDocId", menuDocId);
            i.putExtra("quantity", qty);
            i.putExtra("itemName", productName);
            i.putExtra("itemPrice", productPrice);
            i.putExtra("totalPrice", productPrice * qty);
            if (imageUrl != null) {
                i.putExtra("imageUrl", imageUrl);
            }

            startActivity(i);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error in buyNow: " + e.getMessage(), e);
            Toast.makeText(this, "Error processing order", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to activity
        if (menuDocId != null) {
            Log.d(TAG, "onResume - refreshing reviews and ratings");
            loadProductRatingStats();
            loadReviews();
        }
    }
}