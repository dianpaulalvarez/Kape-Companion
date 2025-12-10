package com.lu.coffeecompanion;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lu.coffeecompanion.databinding.ActivityCartBinding;

import java.util.ArrayList;

public class CartActivity extends AppCompatActivity {

    ActivityCartBinding binding;
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    String userId;

    ArrayList<CartItem> cartItems = new ArrayList<>();
    com.lu.coffeecompanion.CartAdapter cartAdapter;
    boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityCartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        } else {
            userId = currentUser.getUid();
        }

        // SETUP RECYCLERVIEW + ADAPTER WITH QUANTITY CALLBACK
        cartAdapter = new com.lu.coffeecompanion.CartAdapter(this, cartItems, userId, this::updateTotalPrice);
        binding.recyclerViewCart.setAdapter(cartAdapter);
        binding.recyclerViewCart.setLayoutManager(new LinearLayoutManager(this));

        fetchAll();

        // SWIPE REFRESH
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            fetchAll();
            binding.swipeRefreshLayout.setRefreshing(false);
        });

        // BACK BUTTON
        binding.back.setOnClickListener(v -> finish());

        // CHECKOUT BUTTON
        binding.checkout.setOnClickListener(v -> {
            if (isEditMode) {
                Toast.makeText(this, "EXIT EDIT MODE TO CHECKOUT", Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(getApplicationContext(), ChooseAddressActivity.class));
            }
        });

        // EDIT MODE TOGGLE
        binding.editBtn.setOnClickListener(v -> toggleEditMode());

        // SELECT ALL BUTTON
        binding.selectAll.setOnClickListener(v -> {
            for (CartItem item : cartItems) {
                item.setSelected(true);
            }
            cartAdapter.setEditMode(isEditMode); // refresh adapter to show checkboxes
        });

        // DELETE SELECTED BUTTON
        binding.deleteSelected.setOnClickListener(v -> deleteSelectedItems());
    }


    private void toggleEditMode() {
        isEditMode = !isEditMode;
        if (isEditMode) {
            binding.editBtn.setText("DONE");
            binding.editSection.setVisibility(android.view.View.VISIBLE);
        } else {
            binding.editBtn.setText("EDIT");
            binding.editSection.setVisibility(android.view.View.GONE);
            for (CartItem item : cartItems) item.setSelected(false);
        }
        cartAdapter.setEditMode(isEditMode);
    }

    private void fetchAll() {
        cartItems.clear();
        binding.progressBar.setVisibility(android.view.View.VISIBLE);

        db.collection("users").document(userId).collection("cart")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot cartDoc : task.getResult()) {
                            String cartId = cartDoc.getId();
                            String itemRef = cartDoc.getString("docId");
                            double quantity = cartDoc.getDouble("quantity");

                            fetchItemFromShops(itemRef, quantity, cartId);
                        }
                    }
                    binding.progressBar.setVisibility(android.view.View.GONE);
                });
    }

    private void fetchItemFromShops(String docId, double quantity, String cartId) {
        db.collection("shops")
                .get()
                .addOnSuccessListener(shopSnapshots -> {
                    for (DocumentSnapshot shop : shopSnapshots) {
                        String shopId = shop.getId();
                        String shopName = shop.getString("name");

                        db.collection("shops").document(shopId)
                                .collection("menu").document(docId)
                                .get()
                                .addOnSuccessListener(itemDoc -> {
                                    if (itemDoc.exists()) {
                                        String name = itemDoc.getString("name");
                                        double price = itemDoc.getDouble("price");
                                        String imageUrl = itemDoc.getString("imageUrl");

                                        CartItem cartItem = new CartItem(cartId, name, price, shopName, (int) quantity, imageUrl);
                                        cartItems.add(cartItem);
                                        cartAdapter.notifyDataSetChanged();

                                        updateTotalPrice();
                                    }
                                });
                    }
                });
    }

    private void updateTotalPrice() {
        double total = 0.0;
        for (CartItem item : cartItems) {
            total += item.getPrice() * item.getQuantity();
        }
        binding.totalPrice.setText("₱" + String.format("%.2f", total));
    }

    private void deleteSelectedItems() {
        ArrayList<String> selectedIds = new ArrayList<>();
        for (CartItem item : cartItems) {
            if (item.isSelected()) selectedIds.add(item.getCartId());
        }

        if (selectedIds.isEmpty()) {
            Toast.makeText(this, "NO ITEMS SELECTED", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("REMOVE ITEMS")
                .setMessage("ARE YOU SURE WANT TO REMOVE SELECTED ITEMS?")
                .setPositiveButton("YES", (dialog, which) -> {
                    for (String id : selectedIds) {
                        db.collection("users").document(userId)
                                .collection("cart")
                                .document(id)
                                .delete();
                    }
                    Toast.makeText(this, "REMOVED SUCCESSFULLY", Toast.LENGTH_SHORT).show();
                    fetchAll();
                })
                .setNegativeButton("NO", null)
                .show();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        fetchAll();
    }
}
