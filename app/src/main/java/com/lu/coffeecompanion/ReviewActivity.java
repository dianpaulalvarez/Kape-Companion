package com.lu.coffeecompanion;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.*;

public class ReviewActivity extends AppCompatActivity {

    private static final String TAG = "REVIEW_DEBUG";

    // UI Components
    private RatingBar ratingBarOverall;
    private EditText etOverallComment;
    private TextView tvOrderNumber, tvOrderDate;
    private ImageView btnBack;
    private MaterialButton btnSubmit;
    private ViewGroup containerProductRatings;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Data
    private String orderId, userId, userName, shopId;
    private List<OrderItem> orderItems = new ArrayList<>();
    private String firstProductId, firstProductName, firstShopId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_order);

        Log.d(TAG, "=== REVIEW ACTIVITY STARTED ===");

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Check user
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userId = auth.getCurrentUser().getUid();
        Log.d(TAG, "User ID: " + userId);

        // Get intent data
        orderId = getIntent().getStringExtra("orderId");
        shopId = getIntent().getStringExtra("shopId");

        Log.d(TAG, "Order ID: " + orderId);
        Log.d(TAG, "Shop ID from intent: " + shopId);

        if (orderId == null) {
            Toast.makeText(this, "Invalid order!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initViews();

        // Load data
        loadUserName();
        loadOrderDetails();

        // Setup buttons
        btnBack.setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> submitAllRatings());
    }

    private void initViews() {
        ratingBarOverall = findViewById(R.id.ratingBarOverall);
        etOverallComment = findViewById(R.id.etOverallComment);
        tvOrderNumber = findViewById(R.id.tvOrderNumber);
        tvOrderDate = findViewById(R.id.tvOrderDate);
        btnBack = findViewById(R.id.btnBack);
        btnSubmit = findViewById(R.id.btnSubmit);
        containerProductRatings = findViewById(R.id.containerProductRatings);
    }

    private void loadUserName() {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        userName = doc.getString("name");
                        if (TextUtils.isEmpty(userName)) {
                            userName = doc.getString("email");
                        }
                    }
                    if (TextUtils.isEmpty(userName)) {
                        userName = "Anonymous User";
                    }
                    Log.d(TAG, "✅ User name loaded: " + userName);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to load user name", e);
                    userName = "Anonymous User";
                });
    }

    private void loadOrderDetails() {
        Log.d(TAG, "🔄 Loading order details...");

        db.collection("orders").document(orderId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Log.e(TAG, "❌ Order document not found!");
                        Toast.makeText(this, "Order not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    // Check if order is completed
                    String status = doc.getString("status");
                    boolean isCompleted = "completed".equalsIgnoreCase(status) ||
                            "delivered".equalsIgnoreCase(status);

                    if (!isCompleted) {
                        Toast.makeText(this, "You can only rate completed orders", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    // Check if already rated
                    Boolean isRated = doc.getBoolean("isRated");
                    if (isRated != null && isRated) {
                        Toast.makeText(this, "You have already rated this order", Toast.LENGTH_SHORT).show();
                        redirectToProductReviews();
                        return;
                    }

                    // Get shop ID if not from intent
                    if (shopId == null) {
                        shopId = doc.getString("shopId");
                        Log.d(TAG, "Got shopId from order: " + shopId);
                    }

                    // Display order info
                    String receiptNumber = doc.getString("receiptNumber");
                    if (receiptNumber != null && !receiptNumber.isEmpty()) {
                        tvOrderNumber.setText("Order #" + receiptNumber);
                    } else {
                        tvOrderNumber.setText("Order #" + orderId.substring(0, 8).toUpperCase());
                    }

                    Date orderDate = doc.getDate("orderDate");
                    if (orderDate == null) {
                        orderDate = doc.getDate("timestamp");
                    }
                    if (orderDate != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                        tvOrderDate.setText("Ordered on " + sdf.format(orderDate));
                    }

                    // Load order items
                    loadOrderItems();

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to load order details", e);
                    Toast.makeText(this, "Failed to load order", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void loadOrderItems() {
        Log.d(TAG, "🔄 Loading order items...");

        db.collection("orders").document(orderId).collection("items")
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        Log.e(TAG, "❌ No items found in order!");
                        Toast.makeText(this, "No items in this order", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    Log.d(TAG, "✅ Found " + query.size() + " items");

                    for (QueryDocumentSnapshot doc : query) {
                        OrderItem item = new OrderItem();
                        item.productId = doc.getString("productId"); // CRITICAL: MENU DOC ID
                        item.name = doc.getString("name");
                        item.imageUrl = doc.getString("imageUrl");

                        Long qty = doc.getLong("quantity");
                        item.quantity = qty != null ? qty.intValue() : 1;

                        // Get shopId from item or use order's shopId
                        String itemShopId = doc.getString("shopId");
                        item.shopId = itemShopId != null ? itemShopId : shopId;

                        orderItems.add(item);

                        Log.d(TAG, "📦 Item: " + item.name);
                        Log.d(TAG, "   - Product ID: " + item.productId);
                        Log.d(TAG, "   - Shop ID: " + item.shopId);

                        // Store first product for redirection to ProductReviewsActivity
                        if (firstProductId == null && item.productId != null) {
                            firstProductId = item.productId;
                            firstProductName = item.name;
                            firstShopId = item.shopId;
                            Log.d(TAG, "🎯 First product set for ProductReviewsActivity:");
                            Log.d(TAG, "   - ID: " + firstProductId);
                            Log.d(TAG, "   - Name: " + firstProductName);
                            Log.d(TAG, "   - Shop: " + firstShopId);
                        }
                    }

                    createProductCards();

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to load order items", e);
                    Toast.makeText(this, "Failed to load order items", Toast.LENGTH_SHORT).show();
                });
    }

    private void createProductCards() {
        containerProductRatings.removeAllViews();

        if (orderItems.isEmpty()) {
            TextView noItems = new TextView(this);
            noItems.setText("No items in this order");
            noItems.setTextSize(16);
            noItems.setPadding(16, 16, 16, 16);
            containerProductRatings.addView(noItems);
            return;
        }

        for (OrderItem item : orderItems) {
            View view = LayoutInflater.from(this)
                    .inflate(R.layout.item_product_rating, containerProductRatings, false);

            TextView tvProductName = view.findViewById(R.id.tvProductName);
            TextView tvQuantity = view.findViewById(R.id.tvQuantity);
            ImageView ivProduct = view.findViewById(R.id.ivProduct);
            RatingBar ratingBar = view.findViewById(R.id.ratingBarProduct);
            EditText etComment = view.findViewById(R.id.etProductComment);

            tvProductName.setText(item.name);
            tvQuantity.setText("Qty: " + item.quantity);

            // Load image
            if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                Glide.with(this)
                        .load(item.imageUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.ic_menu)
                        .into(ivProduct);
            }

            // Store product ID in tags
            view.setTag(item.productId);
            ratingBar.setTag(item.productId + "_rating");
            etComment.setTag(item.productId + "_comment");

            Log.d(TAG, "📝 Created card for: " + item.name + " (ID: " + item.productId + ")");

            containerProductRatings.addView(view);
        }

        Log.d(TAG, "✅ Created " + orderItems.size() + " product cards");
    }

    private void submitAllRatings() {
        Log.d(TAG, "🔥 SUBMITTING REVIEWS");

        // Validate overall rating
        float overallRating = ratingBarOverall.getRating();
        String overallComment = etOverallComment.getText().toString().trim();

        if (overallRating == 0) {
            Toast.makeText(this, "Please provide an overall rating", Toast.LENGTH_SHORT).show();
            ratingBarOverall.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(overallComment)) {
            Toast.makeText(this, "Please provide an overall comment", Toast.LENGTH_SHORT).show();
            etOverallComment.requestFocus();
            return;
        }

        // Collect product ratings
        Map<String, ProductRating> productRatings = new HashMap<>();

        for (int i = 0; i < containerProductRatings.getChildCount(); i++) {
            View itemView = containerProductRatings.getChildAt(i);
            String productId = (String) itemView.getTag();

            if (productId == null) {
                Log.w(TAG, "⚠️ View tag is null at position " + i);
                continue;
            }

            RatingBar ratingBar = itemView.findViewWithTag(productId + "_rating");
            EditText etComment = itemView.findViewWithTag(productId + "_comment");

            if (ratingBar != null && ratingBar.getRating() > 0) {
                ProductRating pr = new ProductRating();
                pr.rating = ratingBar.getRating();
                pr.comment = etComment.getText().toString().trim();

                if (TextUtils.isEmpty(pr.comment)) {
                    pr.comment = "Great product!";
                }

                productRatings.put(productId, pr);

                Log.d(TAG, "✅ Collected rating for product: " + productId);
                Log.d(TAG, "   - Rating: " + pr.rating);
                Log.d(TAG, "   - Comment: " + pr.comment);
            }
        }

        if (productRatings.isEmpty()) {
            Toast.makeText(this, "Please rate at least one product", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "📊 Total products to rate: " + productRatings.size());

        // Disable button
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting...");

        // Save all ratings
        saveAllRatings(productRatings, overallRating, overallComment);
    }

    private void saveAllRatings(Map<String, ProductRating> productRatings, float overallRating, String overallComment) {
        Log.d(TAG, "💾 SAVING TO FIRESTORE...");

        WriteBatch batch = db.batch();

        // 1. Save overall order rating
        Map<String, Object> orderRatingData = new HashMap<>();
        orderRatingData.put("orderId", orderId);
        orderRatingData.put("userId", userId);
        orderRatingData.put("userName", userName);
        orderRatingData.put("shopId", shopId);
        orderRatingData.put("rating", overallRating);
        orderRatingData.put("comment", overallComment);
        orderRatingData.put("timestamp", FieldValue.serverTimestamp());

        batch.set(db.collection("orderRatings").document(), orderRatingData);
        Log.d(TAG, "➕ Added order rating");

        // 2. Mark order as rated
        Map<String, Object> orderUpdate = new HashMap<>();
        orderUpdate.put("isRated", true);
        orderUpdate.put("lastRatedAt", FieldValue.serverTimestamp());

        batch.update(db.collection("orders").document(orderId), orderUpdate);
        Log.d(TAG, "➕ Updated order as rated");

        // 3. Save each product rating
        for (String productId : productRatings.keySet()) {
            ProductRating pr = productRatings.get(productId);

            Log.d(TAG, "🔥 SAVING PRODUCT RATING:");
            Log.d(TAG, "   📌 productId: " + productId);
            Log.d(TAG, "   ⭐ rating: " + pr.rating);
            Log.d(TAG, "   💬 comment: " + pr.comment);
            Log.d(TAG, "   🧑 userName: " + userName);
            Log.d(TAG, "   🏪 shopId: " + shopId);
            Log.d(TAG, "   📦 orderId: " + orderId);

            Map<String, Object> ratingData = new HashMap<>();
            ratingData.put("productId", productId);
            ratingData.put("userId", userId);
            ratingData.put("userName", userName);
            ratingData.put("shopId", shopId);
            ratingData.put("orderId", orderId);
            ratingData.put("rating", pr.rating);
            ratingData.put("comment", pr.comment);
            ratingData.put("timestamp", FieldValue.serverTimestamp());

            batch.set(db.collection("productRatings").document(), ratingData);
            Log.d(TAG, "✅ Added product rating for: " + productId);
        }

        // Execute batch
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "🎉🎉🎉 BATCH COMMIT SUCCESSFUL! 🎉🎉🎉");
                    Log.d(TAG, "Saved " + productRatings.size() + " product ratings");

                    // Update product statistics
                    for (String productId : productRatings.keySet()) {
                        updateProductStats(productId);
                    }

                    showSuccessAndRedirectToProductReviews();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌❌❌ BATCH COMMIT FAILED! ❌❌❌", e);
                    Log.e(TAG, "Error: " + e.getMessage());

                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Submit Review");
                    Toast.makeText(this, "Failed to submit review: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void updateProductStats(String productId) {
        Log.d(TAG, "🔄 Calculating stats for product: " + productId);

        db.collection("productRatings")
                .whereEqualTo("productId", productId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Log.w(TAG, "No ratings found for product: " + productId);
                        return;
                    }

                    float total = 0;
                    int count = 0;

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Double rating = doc.getDouble("rating");
                        if (rating != null) {
                            total += rating;
                            count++;
                        }
                    }

                    if (count == 0) return;

                    float average = total / count;

                    Map<String, Object> stats = new HashMap<>();
                    stats.put("productId", productId);
                    stats.put("averageRating", average);
                    stats.put("ratingCount", count);
                    stats.put("lastUpdated", FieldValue.serverTimestamp());

                    // Get product name from first rating
                    if (!querySnapshot.getDocuments().isEmpty()) {
                        String name = querySnapshot.getDocuments().get(0).getString("productName");
                        if (name != null) {
                            stats.put("name", name);
                        }
                    }

                    int finalCount = count;
                    db.collection("products").document(productId)
                            .set(stats, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "✅ Updated product stats:");
                                Log.d(TAG, "   - Product: " + productId);
                                Log.d(TAG, "   - Average: " + String.format("%.1f", average));
                                Log.d(TAG, "   - Count: " + finalCount);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ Failed to update product stats", e);
                            });

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to calculate product stats", e);
                });
    }

    private void showSuccessAndRedirectToProductReviews() {
        Log.d(TAG, "✅ Showing success message...");

        Toast.makeText(this, "🎉 Thank you for your review!", Toast.LENGTH_LONG).show();

        // Wait 1.5 seconds then redirect to ProductReviewsActivity
        new android.os.Handler().postDelayed(() -> {
            redirectToProductReviews();
        }, 1500);
    }

    private void redirectToProductReviews() {
        Log.d(TAG, "📱 REDIRECTING TO PRODUCT REVIEWS ACTIVITY");

        if (firstProductId == null) {
            Log.e(TAG, "❌ Missing product info for redirect!");

            // Try to get from order items
            if (!orderItems.isEmpty()) {
                OrderItem firstItem = orderItems.get(0);
                firstProductId = firstItem.productId;
                firstProductName = firstItem.name;
                Log.d(TAG, "📦 Using first item from list");
            } else {
                goBackToOrders();
                return;
            }
        }

        Log.d(TAG, "🎯 Redirecting to ProductReviewsActivity:");
        Log.d(TAG, "   - Product ID: " + firstProductId);
        Log.d(TAG, "   - Product Name: " + firstProductName);

        Intent intent = new Intent(ReviewActivity.this, ProductReviewsActivity.class);
        intent.putExtra("productId", firstProductId);
        intent.putExtra("productName", firstProductName);

        // Clear back stack para hindi makabalik sa review activity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }

    private void redirectToFirstProduct() {
        // This method is for redirecting to ItemActivity (if needed)
        Log.d(TAG, "📱 REDIRECTING TO ITEM ACTIVITY");

        if (firstProductId == null || firstShopId == null) {
            Log.e(TAG, "❌ Missing product info for redirect!");
            goBackToOrders();
            return;
        }

        Intent intent = new Intent(ReviewActivity.this, ItemActivity.class);
        intent.putExtra("docId", firstProductId);
        intent.putExtra("documentId", firstShopId);
        if (firstProductName != null) {
            intent.putExtra("productName", firstProductName);
        }

        startActivity(intent);
        finish();
    }

    private void goBackToOrders() {
        Log.d(TAG, "↩️ Going back to orders");

        Intent intent = new Intent(ReviewActivity.this, OrdersActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    // Helper classes
    static class OrderItem {
        String productId;
        String name;
        String imageUrl;
        String shopId;
        int quantity;
    }

    static class ProductRating {
        float rating;
        String comment;
    }
}