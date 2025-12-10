package com.lu.coffeecompanion;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.lu.coffeecompanion.databinding.ActivityOrderDetailsBinding;
import com.lu.coffeecompanion.models.Review;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OrderDetailsActivity extends AppCompatActivity {

    private static final String TAG = "OrderDetailsActivity";
    private static final long CANCEL_WINDOW = 3 * 60 * 1000; // 3 minutes in milliseconds
    private static final String SUPPORT_PHONE_NUMBER = "09999999999";

    private ActivityOrderDetailsBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;
    private String orderId;
    private ListenerRegistration statusListener;
    private Handler countdownHandler;
    private Runnable countdownRunnable;
    private long orderTimestampMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeFirebase();

        if (!checkUserAuthentication()) {
            return;
        }

        orderId = getIntent().getStringExtra("orderId");
        if (orderId == null) {
            Toast.makeText(this, "Invalid order", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
        loadOrderDetails();
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private boolean checkUserAuthentication() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return false;
        }
        userId = currentUser.getUid();
        return true;
    }

    private void setupUI() {
        // Back button functionality
        binding.back.setOnClickListener(v -> finish());

        // Contact Support button
        binding.btnContactSupport.setOnClickListener(v -> showContactSupportDialog());
    }

    private void loadOrderDetails() {
        showLoading(true);

        // Real-time listener for status updates
        statusListener = db.collection("orders")
                .document(orderId)
                .addSnapshotListener((document, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading order", error);
                        showLoading(false);
                        Toast.makeText(this, "Failed to load order", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (document != null && document.exists()) {
                        displayOrderDetails(document);
                        loadOrderItems();
                        startCancelCountdown(document);
                    } else {
                        showLoading(false);
                        Toast.makeText(this, "Order not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void displayOrderDetails(DocumentSnapshot document) {
        try {
            // Basic order info
            String name = document.getString("name");
            String address = document.getString("address");
            String mobile = document.getString("mobile");
            String status = document.getString("status");
            String paymentMethod = document.getString("paymentMethod");
            String receiptNumber = document.getString("receiptNumber");
            Double subtotalValue = document.getDouble("subtotal");
            Double deliveryFeeValue = document.getDouble("deliveryFee");
            Double totalPrice = document.getDouble("totalPrice");
            Timestamp orderTimestamp = document.getTimestamp("orderTimestamp");

            // Set basic info
            binding.name.setText(name != null ? name : "N/A");
            binding.address.setText(address != null ? address : "N/A");
            binding.mobile.setText(mobile != null ? mobile : "N/A");

            // Set status with color
            binding.status.setText(status != null ? status.toUpperCase() : "PENDING");
            setStatusColor(binding.status, status);

            // Set order date
            if (orderTimestamp != null) {
                Date date = orderTimestamp.toDate();
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy - h:mm a", Locale.getDefault());
                binding.date.setText(dateFormat.format(date));
                orderTimestampMillis = orderTimestamp.toDate().getTime();
            }

            // Set payment method
            binding.paymentMethod.setText(paymentMethod != null ? paymentMethod : "N/A");

            // Set receipt number
            if (receiptNumber != null) {
                binding.receiptNumber.setText(receiptNumber);
            } else {
                binding.receiptNumber.setText("N/A");
            }

            // CALCULATE ORDER SUMMARY
            calculateAndDisplayOrderSummary(subtotalValue, deliveryFeeValue, totalPrice);

        } catch (Exception e) {
            Log.e(TAG, "Error displaying order details", e);
        }
    }

    private void calculateAndDisplayOrderSummary(Double subtotalValue, Double deliveryFeeValue, Double totalPrice) {
        // For backward compatibility - if subtotal is null, calculate it
        double subtotal = 0.0;
        double deliveryFee = 0.0;
        double total = 0.0;

        if (totalPrice != null) {
            total = totalPrice;

            if (subtotalValue != null) {
                subtotal = subtotalValue;
            } else {
                // Calculate subtotal from total and delivery fee
                if (deliveryFeeValue != null) {
                    deliveryFee = deliveryFeeValue;
                    subtotal = total - deliveryFee;
                } else {
                    // Default delivery fee
                    deliveryFee = 30.0;
                    subtotal = total - deliveryFee;
                }
            }

            if (deliveryFeeValue != null) {
                deliveryFee = deliveryFeeValue;
            } else {
                deliveryFee = 30.0; // Default delivery fee
            }
        } else {
            // Old order format - use subtotal and delivery fee
            subtotal = subtotalValue != null ? subtotalValue : 0.0;
            deliveryFee = deliveryFeeValue != null ? deliveryFeeValue : 0.0;
            total = subtotal + deliveryFee;
        }

        // Display order summary
        binding.subtotal.setText(String.format("₱%.2f", subtotal));
        binding.deliveryFee.setText(String.format("₱%.2f", deliveryFee));
        binding.totalCost.setText(String.format("₱%.2f", total));

        Log.d(TAG, "Order Summary - Subtotal: " + subtotal +
                ", Delivery: " + deliveryFee +
                ", Total: " + total);
    }

    private void setStatusColor(TextView tvStatus, String status) {
        if (status == null) status = "pending";

        int backgroundColor;
        switch (status.toLowerCase()) {
            case "pending":
                backgroundColor = 0xFFFFC107;
                break;
            case "preparing":
            case "confirmed":
                backgroundColor = 0xFF2196F3;
                break;
            case "out for delivery":
                backgroundColor = 0xFFFF9800;
                break;
            case "delivered":
            case "completed":
                backgroundColor = 0xFF4CAF50;
                break;
            case "cancelled":
                backgroundColor = 0xFFF44336;
                break;
            default:
                backgroundColor = 0xFFFFC107;
        }

        tvStatus.setBackgroundColor(backgroundColor);
        tvStatus.setTextColor(0xFFFFFFFF);
        tvStatus.setPadding(24, 12, 24, 12);
    }

    private void loadOrderItems() {
        showLoading(true);
        binding.itemContainer.removeAllViews();

        db.collection("orders")
                .document(orderId)
                .collection("items")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        showLoading(false);
                        return;
                    }

                    for (DocumentSnapshot itemDoc : querySnapshot.getDocuments()) {
                        String docId = itemDoc.getString("docId");
                        Double quantity = itemDoc.getDouble("quantity");

                        if (docId != null && quantity != null) {
                            loadMenuItem(docId, quantity);
                        }
                    }

                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading items", e);
                    showLoading(false);
                });
    }

    private void loadMenuItem(String menuItemId, Double quantity) {
        db.collection("shops")
                .get()
                .addOnSuccessListener(shopsSnapshot -> {
                    for (DocumentSnapshot shop : shopsSnapshot.getDocuments()) {
                        String shopId = shop.getId();

                        db.collection("shops")
                                .document(shopId)
                                .collection("menu")
                                .document(menuItemId)
                                .get()
                                .addOnSuccessListener(menuItem -> {
                                    if (menuItem.exists()) {
                                        String itemName = menuItem.getString("name");
                                        Double price = menuItem.getDouble("price");
                                        String imageUrl = menuItem.getString("imageUrl");

                                        if (itemName != null && price != null) {
                                            displayMenuItem(itemName, price, imageUrl, quantity, menuItemId);
                                        }
                                    }
                                });
                    }
                });
    }

    private void displayMenuItem(String itemName, Double price, String imageUrl, Double quantity, String menuItemId) {
        if (binding.itemContainer == null) return;

        View orderView = LayoutInflater.from(this)
                .inflate(R.layout.item_orderitems, binding.itemContainer, false);

        ImageView imageItem = orderView.findViewById(R.id.img_item);
        TextView productName = orderView.findViewById(R.id.product_name);
        TextView productPrice = orderView.findViewById(R.id.product_price);
        TextView quantityText = orderView.findViewById(R.id.quantity);
        Button rateBtn = orderView.findViewById(R.id.rate_btn);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.image_placeholder)
                    .into(imageItem);
        }

        productName.setText(itemName);
        productPrice.setText(String.format("₱%.2f", price));
        quantityText.setText(String.format("x%.0f", quantity));

        // Check if already rated
        checkIfAlreadyRated(orderId, menuItemId, rateBtn);

        // Rating button click
        rateBtn.setOnClickListener(v -> {
            db.collection("orders").document(orderId).get().addOnSuccessListener(doc -> {
                String status = doc.getString("status");
                if (status != null && status.equalsIgnoreCase("completed")) {
                    RatingDialog.showRatingDialog(this, orderId, menuItemId, itemName,
                            (oId, mId, name, rating, feedback) -> submitRating(oId, mId, name, rating, feedback));
                } else {
                    Toast.makeText(this, "Order must be completed to rate", Toast.LENGTH_SHORT).show();
                }
            });
        });

        binding.itemContainer.addView(orderView);
    }

    private void checkIfAlreadyRated(String orderId, String menuItemId, Button rateBtn) {
        db.collection("reviews")
                .whereEqualTo("orderId", orderId)
                .whereEqualTo("menuItemId", menuItemId)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        rateBtn.setText("★ Already Rated");
                        rateBtn.setEnabled(false);
                        rateBtn.setAlpha(0.5f);
                    } else {
                        rateBtn.setText("Rate Item");
                        rateBtn.setEnabled(true);
                        rateBtn.setAlpha(1f);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking rating", e));
    }

    private void submitRating(String orderId, String menuItemId, String itemName, float rating, String feedback) {
        String userName = binding.name.getText().toString();

        Review review = new Review(orderId, menuItemId, itemName, userId, userName, rating, feedback);

        db.collection("reviews").add(review)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Rating submitted successfully", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Review added: " + documentReference.getId());
                    loadOrderDetails();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to submit rating: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error submitting review", e);
                });
    }

    private void showContactSupportDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Contact Support")
                .setMessage("Need help with your order? You can:\n\n1. Call support at " + SUPPORT_PHONE_NUMBER + "\n2. Send an email to support@coffeecompanion.com")
                .setPositiveButton("Call Support", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + SUPPORT_PHONE_NUMBER));
                    startActivity(intent);
                })
                .setNeutralButton("Copy Phone", (dialog, which) -> {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Support Phone", SUPPORT_PHONE_NUMBER);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Phone number copied to clipboard", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // NEW: Start countdown timer for cancel button
    private void startCancelCountdown(DocumentSnapshot document) {
        String status = document.getString("status");

        // Only show cancel button if status is PENDING
        if (status == null || !status.equalsIgnoreCase("pending")) {
            binding.cancelOrderBtn.setVisibility(View.GONE);
            return;
        }

        // Get timestamp
        Timestamp orderTimestamp = document.getTimestamp("orderTimestamp");
        if (orderTimestamp != null) {
            orderTimestampMillis = orderTimestamp.toDate().getTime();
        }

        // Set up countdown
        countdownHandler = new Handler(Looper.getMainLooper());
        updateCancelButtonState();
    }

    private void updateCancelButtonState() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - orderTimestampMillis;
        long remainingTime = CANCEL_WINDOW - elapsedTime;

        if (remainingTime > 0) {
            // Still within 3 minutes - show cancel button
            long secondsRemaining = remainingTime / 1000;
            binding.cancelOrderBtn.setText(String.format("Cancel Order (%d:%02d)", secondsRemaining / 60, secondsRemaining % 60));
            binding.cancelOrderBtn.setEnabled(true);
            binding.cancelOrderBtn.setVisibility(View.VISIBLE);

            binding.cancelOrderBtn.setOnClickListener(v -> showCancelConfirmation());

            Log.d(TAG, "Cancel button visible - " + secondsRemaining + " seconds remaining");

            // Schedule next update
            if (countdownRunnable != null) {
                countdownHandler.removeCallbacks(countdownRunnable);
            }
            countdownRunnable = () -> updateCancelButtonState();
            countdownHandler.postDelayed(countdownRunnable, 1000);
        } else {
            // Over 3 minutes - hide cancel button
            binding.cancelOrderBtn.setVisibility(View.GONE);
            Log.d(TAG, "Cancel button hidden - time expired");
            if (countdownHandler != null && countdownRunnable != null) {
                countdownHandler.removeCallbacks(countdownRunnable);
            }
        }
    }

    private void showCancelConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Order")
                .setMessage("Are you sure you want to cancel this order?")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> cancelOrder())
                .setNegativeButton("No, Keep Order", null)
                .show();
    }

    private void cancelOrder() {
        db.collection("orders")
                .document(orderId)
                .update("status", "cancelled")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Order cancelled successfully", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Order cancelled: " + orderId);
                    binding.cancelOrderBtn.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to cancel order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error cancelling order", e);
                });
    }

    private void showLoading(boolean show) {
        // Implement loading indicator if you have one in your layout
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (statusListener != null) {
            statusListener.remove();
        }
        if (countdownHandler != null && countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }
    }
}