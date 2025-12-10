package com.lu.coffeecompanion;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.lu.coffeecompanion.databinding.ActivityAddressListBinding;

public class AddressListActivity extends AppCompatActivity {
    ActivityAddressListBinding binding;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseAuth auth = FirebaseAuth.getInstance();
    FirebaseUser currentUser = auth.getCurrentUser();

    // Variables for Buy Now flow
    private boolean isBuyNowFlow = false;
    private String shopId;
    private String itemDocId;
    private int quantity;
    private String itemName;
    private double itemPrice;
    private double totalPrice;
    private String orderType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityAddressListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Debug log
        Log.d("AddressListActivity", "onCreate called");

        // Check if this is a Buy Now flow from ItemActivity
        checkBuyNowIntent();

        loadAddresses();

        binding.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadAddresses();
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        });

        binding.back.setOnClickListener(v -> {
            finish();
        });

        binding.addAddress.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), AddAddressActivity.class);
            if (isBuyNowFlow) {
                intent.putExtra("isBuyNowFlow", true);
                intent.putExtra("shopId", shopId);
                intent.putExtra("itemDocId", itemDocId);
                intent.putExtra("quantity", quantity);
                intent.putExtra("itemName", itemName);
                intent.putExtra("itemPrice", itemPrice);
                intent.putExtra("totalPrice", totalPrice);
                intent.putExtra("orderType", orderType);
            }
            startActivity(intent);
        });
    }

    private void checkBuyNowIntent() {
        Intent intent = getIntent();
        orderType = intent.getStringExtra("orderType");

        Log.d("AddressListActivity", "Order Type received: " + orderType);

        if (orderType != null && orderType.equals("buy_now")) {
            isBuyNowFlow = true;
            shopId = intent.getStringExtra("shopId");
            itemDocId = intent.getStringExtra("itemDocId");
            quantity = intent.getIntExtra("quantity", 1);
            itemName = intent.getStringExtra("itemName");
            itemPrice = intent.getDoubleExtra("itemPrice", 0.0);
            totalPrice = intent.getDoubleExtra("totalPrice", 0.0);

            Log.d("AddressListActivity", "Buy Now Flow Activated");
            Log.d("AddressListActivity", "Shop ID: " + shopId);
            Log.d("AddressListActivity", "Item Doc ID: " + itemDocId);

            // Update UI for Buy Now flow
            if (binding.instructionText != null) {
                binding.instructionText.setVisibility(View.VISIBLE);
            }
            if (binding.selectAddressButton != null) {
                binding.selectAddressButton.setVisibility(View.VISIBLE);
                binding.selectAddressButton.setOnClickListener(v -> {
                    Toast.makeText(this, "Please select an address from the list", Toast.LENGTH_SHORT).show();
                });
            }

            // Update title for Buy Now flow
            if (binding.title != null) {
                binding.title.setText("Select Delivery Address");
            }
        } else {
            isBuyNowFlow = false;
            if (binding.instructionText != null) {
                binding.instructionText.setVisibility(View.GONE);
            }
            if (binding.selectAddressButton != null) {
                binding.selectAddressButton.setVisibility(View.GONE);
            }
            if (binding.title != null) {
                binding.title.setText("Addresses");
            }
        }
    }

    private void loadAddresses() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.parentLayout.removeAllViews();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("users").document(userId).collection("addresses")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                for (QueryDocumentSnapshot document : querySnapshot) {
                                    String documentId = document.getId();
                                    String name = document.getString("name");
                                    String address = document.getString("address");
                                    String mobile = document.getString("mobile");
                                    String city = document.getString("city");
                                    String province = document.getString("province");
                                    String zipcode = document.getString("zipcode");

                                    addAddressView(documentId, address, mobile, name, city, province, zipcode);
                                }
                            } else {
                                showNoAddressesView();
                            }
                            binding.progressBar.setVisibility(View.GONE);
                        } else {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, "Failed to load addresses", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    private void addAddressView(String documentId, String address, String mobile, String name,
                                String city, String province, String zipcode) {
        View addressView = LayoutInflater.from(this).inflate(R.layout.item_addresscard, null);

        TextView addressTextView = addressView.findViewById(R.id.addressTextView);
        TextView mobileTextView = addressView.findViewById(R.id.mobileTextView);
        TextView nameTextView = addressView.findViewById(R.id.nameTextView);
        Button selectButton = addressView.findViewById(R.id.selectButton);
        ImageView pencilIcon = addressView.findViewById(R.id.pencil);

        // Set text values
        addressTextView.setText(address != null ? address : "");
        mobileTextView.setText(mobile != null ? mobile : "");
        nameTextView.setText(name != null ? name : "");

        if (isBuyNowFlow) {
            // For Buy Now flow - hide pencil, show select button
            pencilIcon.setVisibility(View.GONE);
            selectButton.setVisibility(View.VISIBLE);

            // Make entire card clickable
            addressView.setClickable(true);
            addressView.setOnClickListener(v -> {
                Log.d("AddressListActivity", "Address card clicked for buy now");
                goToCheckoutWithAddress(documentId, name, address, mobile, city, province, zipcode);
            });

            // Select button click
            selectButton.setOnClickListener(v -> {
                Log.d("AddressListActivity", "Select button clicked");
                goToCheckoutWithAddress(documentId, name, address, mobile, city, province, zipcode);
            });

        } else {
            // For normal flow - show pencil, hide select button
            selectButton.setVisibility(View.GONE);
            pencilIcon.setVisibility(View.VISIBLE);

            // Card click for editing
            addressView.setOnClickListener(v -> {
                Intent intent = new Intent(getApplicationContext(), EditAddressActivity.class);
                intent.putExtra("documentId", documentId);
                startActivity(intent);
            });

            // Pencil icon click
            pencilIcon.setOnClickListener(v -> {
                Intent intent = new Intent(getApplicationContext(), EditAddressActivity.class);
                intent.putExtra("documentId", documentId);
                startActivity(intent);
            });
        }

        binding.parentLayout.addView(addressView);
    }

    private void goToCheckoutWithAddress(String addressDocId, String name, String address,
                                         String mobile, String city, String province, String zipcode) {
        Log.d("AddressListActivity", "goToCheckoutWithAddress called");

        // Create full address
        StringBuilder fullAddressBuilder = new StringBuilder();
        if (address != null) fullAddressBuilder.append(address);
        if (city != null && !city.isEmpty()) {
            if (fullAddressBuilder.length() > 0) fullAddressBuilder.append(", ");
            fullAddressBuilder.append(city);
        }
        if (province != null && !province.isEmpty()) {
            if (fullAddressBuilder.length() > 0) fullAddressBuilder.append(", ");
            fullAddressBuilder.append(province);
        }
        if (zipcode != null && !zipcode.isEmpty()) {
            if (fullAddressBuilder.length() > 0) fullAddressBuilder.append(" ");
            fullAddressBuilder.append(zipcode);
        }

        String fullAddress = fullAddressBuilder.toString();

        // Create CheckoutActivity intent
        Intent intent = new Intent(AddressListActivity.this, CheckoutActivity.class);

        // Add Buy Now data
        intent.putExtra("orderType", "buy_now");
        intent.putExtra("shopId", shopId);
        intent.putExtra("itemDocId", itemDocId);
        intent.putExtra("quantity", quantity);
        intent.putExtra("itemName", itemName);
        intent.putExtra("itemPrice", itemPrice);
        intent.putExtra("totalPrice", totalPrice);

        // Add address data
        intent.putExtra("addressDocId", addressDocId);
        intent.putExtra("recipientName", name);
        intent.putExtra("deliveryAddress", fullAddress);
        intent.putExtra("mobileNumber", mobile);
        intent.putExtra("city", city);
        intent.putExtra("province", province);
        intent.putExtra("zipcode", zipcode);

        // Clear back stack and start activity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        // Show success message
        Toast.makeText(this, "Address selected. Going to checkout...", Toast.LENGTH_SHORT).show();

        Log.d("AddressListActivity", "Starting CheckoutActivity with data:");
        Log.d("AddressListActivity", "Item: " + itemName);
        Log.d("AddressListActivity", "Quantity: " + quantity);
        Log.d("AddressListActivity", "Address: " + fullAddress);

        startActivity(intent);
        finish();
    }

    private void showNoAddressesView() {
        TextView noAddressTextView = new TextView(this);
        noAddressTextView.setText("No addresses found. Please add an address.");
        noAddressTextView.setTextSize(16);
        noAddressTextView.setPadding(16, 32, 16, 32);
        noAddressTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        if (isBuyNowFlow) {
            noAddressTextView.setText("No addresses found. Please add an address to continue with your purchase.");
            noAddressTextView.setOnClickListener(v -> {
                Intent intent = new Intent(getApplicationContext(), AddAddressActivity.class);
                intent.putExtra("isBuyNowFlow", true);
                intent.putExtra("shopId", shopId);
                intent.putExtra("itemDocId", itemDocId);
                intent.putExtra("quantity", quantity);
                intent.putExtra("itemName", itemName);
                intent.putExtra("itemPrice", itemPrice);
                intent.putExtra("totalPrice", totalPrice);
                intent.putExtra("orderType", orderType);
                startActivity(intent);
            });
        }

        binding.parentLayout.addView(noAddressTextView);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        loadAddresses();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkBuyNowIntent();
        loadAddresses();
    }
}