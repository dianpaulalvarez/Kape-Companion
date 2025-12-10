package com.lu.coffeecompanion;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lu.coffeecompanion.databinding.ActivityViewReceiptBinding;
import com.lu.coffeecompanion.databinding.ItemReceiptItemBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ViewReceiptActivity extends AppCompatActivity {

    private static final String TAG = "ViewReceiptActivity";

    private ActivityViewReceiptBinding binding;
    private FirebaseFirestore db;
    private String orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityViewReceiptBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        orderId = getIntent().getStringExtra("orderId");

        setupUI();
        loadReceipt();
    }

    private void setupUI() {
        binding.back.setOnClickListener(v -> finish());
    }

    private void loadReceipt() {
        if (orderId == null) {
            Toast.makeText(this, "Invalid order", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showLoading(true);

        db.collection("orders")
                .document(orderId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        displayReceipt(document);
                        loadOrderItems();
                    } else {
                        showLoading(false);
                        Toast.makeText(this, "Receipt not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error loading receipt", e);
                    Toast.makeText(this, "Failed to load receipt", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void displayReceipt(DocumentSnapshot document) {
        String receiptNumber = document.getString("receiptNumber");
        String customerName = document.getString("name");
        String contactNumber = document.getString("mobile");
        String address = document.getString("address");
        String paymentMethod = document.getString("paymentMethod");

        Double totalPrice = document.getDouble("totalPrice");
        Double deliveryFee = document.getDouble("deliveryFee");

        double subtotal = totalPrice != null ? totalPrice : 0.0;
        double delivery = deliveryFee != null ? deliveryFee : 0.0;
        double total = subtotal + delivery;

        Date orderDate = document.getTimestamp("orderTimestamp") != null
                ? document.getTimestamp("orderTimestamp").toDate()
                : new Date();

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        String formattedDate = dateFormat.format(orderDate);

        binding.receiptNumber.setText(receiptNumber != null ? receiptNumber : "N/A");
        binding.customerName.setText(customerName != null ? customerName : "N/A");
        binding.contactNumber.setText(contactNumber != null ? contactNumber : "N/A");
        binding.orderDate.setText(formattedDate);
        binding.paymentMethod.setText(paymentMethod != null ? paymentMethod : "N/A");
        binding.deliveryAddress.setText(address != null ? address : "N/A");

        binding.subtotalAmount.setText(String.format("₱%.2f", subtotal));
        binding.deliveryAmount.setText(String.format("₱%.2f", delivery));
        binding.totalAmount.setText(String.format("₱%.2f", total));
    }

    private void loadOrderItems() {
        binding.itemsContainer.removeAllViews();

        db.collection("orders")
                .document(orderId).collection("items")
                .get()
                .addOnSuccessListener(itemsSnapshot -> {
                    if (itemsSnapshot.isEmpty()) {
                        showLoading(false);
                        return;
                    }

                    for (DocumentSnapshot item : itemsSnapshot.getDocuments()) {
                        String docId = item.getString("docId");
                        Double quantity = item.getDouble("quantity");

                        if (docId != null && quantity != null) {
                            loadMenuItem(docId, quantity);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error loading items", e);
                });
    }

    private void loadMenuItem(String docId, Double quantity) {
        db.collection("shops")
                .get()
                .addOnSuccessListener(shopsSnapshot -> {
                    for (DocumentSnapshot shop : shopsSnapshot.getDocuments()) {
                        String shopId = shop.getId();

                        db.collection("shops")
                                .document(shopId)
                                .collection("menu")
                                .document(docId)
                                .get()
                                .addOnSuccessListener(menuItem -> {
                                    if (menuItem.exists()) {
                                        displayMenuItem(menuItem, quantity);
                                    }
                                });
                    }
                    showLoading(false);
                });
    }

    private void displayMenuItem(DocumentSnapshot menuItem, Double quantity) {
        String itemName = menuItem.getString("name");
        Double itemPrice = menuItem.getDouble("price");

        if (itemName == null || itemPrice == null) {
            return;
        }

        double subtotal = itemPrice * quantity;

        ItemReceiptItemBinding itemBinding = ItemReceiptItemBinding.inflate(getLayoutInflater());

        String itemText = String.format("%s x%d — ₱%.2f each",
                itemName, quantity.intValue(), itemPrice);

        itemBinding.itemDescription.setText(itemText);
        itemBinding.itemSubtotal.setText(String.format("₱%.2f", subtotal));

        binding.itemsContainer.addView(itemBinding.getRoot());
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.scrollView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}