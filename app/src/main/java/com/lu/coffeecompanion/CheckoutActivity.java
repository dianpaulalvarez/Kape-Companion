package com.lu.coffeecompanion;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.lu.coffeecompanion.databinding.ActivityCheckoutBinding;
import com.lu.coffeecompanion.databinding.DialogGcashQrBinding;
import com.lu.coffeecompanion.databinding.ItemCartBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CheckoutActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_PICK = 200;
    private static final String TAG = "CheckoutActivity";
    private static final double DELIVERY_FEE = 30.0;

    private ActivityCheckoutBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseDatabase realtimeDb;
    private String userId;
    private String selectedPaymentMethod = "";
    private Uri proofImageUri = null;
    private boolean hasPaidWithGcash = false;
    private boolean isFirstTimeBuyer = true;
    private String addressDocId;

    private double totalPrice = 0.0;
    private List<Map<String, Object>> cartItems = new ArrayList<>();
    private Map<String, Object> addressData = new HashMap<>();
    private String userName = "";

    // NEW: Variables for Buy Now flow
    private boolean isBuyNowFlow = false;
    private String shopId;
    private String itemDocId;
    private int quantity;
    private String itemName;
    private double itemPrice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityCheckoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        applyWindowInsets();
        initializeFirebase();

        if (!checkUserAuthentication()) {
            return;
        }

        // NEW: Check if this is Buy Now flow or Cart flow
        checkOrderType();

        setupUI();
        checkFirstTimeBuyer();
        loadCheckoutData();
        loadUserName();
    }

    // NEW: Method to check order type
    private void checkOrderType() {
        Intent intent = getIntent();
        String orderType = intent.getStringExtra("orderType");

        if (orderType != null && orderType.equals("buy_now")) {
            isBuyNowFlow = true;
            shopId = intent.getStringExtra("shopId");
            itemDocId = intent.getStringExtra("itemDocId");
            quantity = intent.getIntExtra("quantity", 1);
            itemName = intent.getStringExtra("itemName");
            itemPrice = intent.getDoubleExtra("itemPrice", 0.0);
            totalPrice = intent.getDoubleExtra("totalPrice", 0.0);

            // Get address from Buy Now flow
            addressDocId = intent.getStringExtra("addressDocId");
            addressData.put("name", intent.getStringExtra("recipientName"));
            addressData.put("address", intent.getStringExtra("deliveryAddress"));
            addressData.put("mobile", intent.getStringExtra("mobileNumber"));

            Log.d(TAG, "Buy Now Flow Activated");
            Log.d(TAG, "Item: " + itemName + ", Quantity: " + quantity + ", Price: " + totalPrice);
        } else {
            // Regular cart flow
            isBuyNowFlow = false;
            addressDocId = getIntent().getStringExtra("documentId");
        }
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        realtimeDb = FirebaseDatabase.getInstance();
    }

    private boolean checkUserAuthentication() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            navigateToLogin();
            return false;
        }
        userId = currentUser.getUid();
        return true;
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void setupUI() {
        binding.proofContainer.setVisibility(View.GONE);
        binding.back.setOnClickListener(v -> finish());

        binding.radioGroupPayment.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioGcash) {
                handleGcashSelection();
            } else if (checkedId == R.id.radioCod) {
                handleCodSelection();
            }
        });

        binding.btnUploadProof.setOnClickListener(v -> openImagePicker());
        binding.placeOrder.setOnClickListener(v -> validateAndPlaceOrder());
    }

    private void loadUserName() {
        DatabaseReference userRef = realtimeDb.getReference("users").child(userId);
        userRef.child("name").addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    userName = dataSnapshot.getValue(String.class);
                    if (userName == null || userName.isEmpty()) {
                        userName = "Customer";
                    }
                } else {
                    userName = "Customer";
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError databaseError) {
                Log.e(TAG, "Failed to load user name");
                userName = "Customer";
            }
        });
    }

    private void checkFirstTimeBuyer() {
        db.collection("orders")
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    isFirstTimeBuyer = querySnapshot.isEmpty();
                    updatePaymentMethodAvailability();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking buyer status", e);
                    isFirstTimeBuyer = true;
                    updatePaymentMethodAvailability();
                });
    }

    private void updatePaymentMethodAvailability() {
        if (isFirstTimeBuyer) {
            binding.radioCod.setEnabled(false);
            binding.radioCod.setAlpha(0.5f);
            binding.radioCod.setText("Cash on Delivery (Available after first order)");
            binding.radioGcash.setChecked(true);
        } else {
            binding.radioCod.setEnabled(true);
            binding.radioCod.setAlpha(1.0f);
            binding.radioCod.setText("Cash on Delivery");
        }
    }

    private void loadCheckoutData() {
        if (isBuyNowFlow) {
            // NEW: Load data for Buy Now flow
            loadBuyNowData();
        } else {
            // Regular cart flow
            loadAddress();
            loadCartItems();
        }
    }

    // NEW: Method to load Buy Now data
    private void loadBuyNowData() {
        showLoading(true);
        binding.itemContainer.removeAllViews();

        // Display the single item for Buy Now
        displayBuyNowItem();

        // Update order summary
        updateOrderSummary();

        showLoading(false);
    }

    // NEW: Method to display single item for Buy Now
    private void displayBuyNowItem() {
        // Clear container first
        binding.itemContainer.removeAllViews();

        ItemCartBinding cartBinding = ItemCartBinding.inflate(getLayoutInflater());

        // Load item image from Firestore
        if (shopId != null && itemDocId != null) {
            db.collection("shops")
                    .document(shopId)
                    .collection("menu")
                    .document(itemDocId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String imageUrl = document.getString("imageUrl");
                            final String[] shopName = {"Loading..."};

                            // Get shop name
                            db.collection("shops").document(shopId).get()
                                    .addOnSuccessListener(shopDoc -> {
                                        if (shopDoc.exists()) {
                                            shopName[0] = shopDoc.getString("name");
                                            cartBinding.itemShop.setText(shopName[0]);
                                        }
                                    });

                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.image_placeholder)
                                        .into(cartBinding.itemImage);
                            }
                        }
                    });
        }

        cartBinding.itemName.setText(itemName);
        cartBinding.itemPrice.setText(String.format("₱%.2f", itemPrice));
        cartBinding.itemShop.setText("Loading shop...");
        cartBinding.quantity.setText(String.valueOf(quantity));

        binding.itemContainer.addView(cartBinding.getRoot());
    }

    private void loadAddress() {
        if (addressDocId == null) {
            Toast.makeText(this, "No address selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("users")
                .document(userId)
                .collection("addresses")
                .document(addressDocId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        addressData.put("name", document.getString("name"));
                        addressData.put("address", document.getString("address"));
                        addressData.put("mobile", document.getString("mobile"));
                    } else {
                        Toast.makeText(this, "Address not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading address", e);
                    Toast.makeText(this, "Failed to load address", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void loadCartItems() {
        showLoading(true);
        binding.itemContainer.removeAllViews();
        totalPrice = 0.0;
        cartItems.clear();

        db.collection("users")
                .document(userId)
                .collection("cart")
                .get()
                .addOnSuccessListener(cartSnapshot -> {
                    if (cartSnapshot.isEmpty()) {
                        showLoading(false);
                        Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    List<String> menuItemIds = new ArrayList<>();
                    Map<String, Double> quantityMap = new HashMap<>();

                    for (DocumentSnapshot doc : cartSnapshot.getDocuments()) {
                        String docId = doc.getString("docId");
                        String shopIdFromCart = doc.getString("shopId");
                        Double quantity = doc.getDouble("quantity");

                        if (docId != null && quantity != null) {
                            menuItemIds.add(docId);
                            quantityMap.put(docId, quantity);

                            Map<String, Object> item = new HashMap<>();
                            item.put("docId", docId);
                            item.put("shopId", shopIdFromCart);
                            item.put("quantity", quantity);
                            cartItems.add(item);
                        }
                    }

                    loadMenuItems(menuItemIds, quantityMap);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error loading cart", e);
                    Toast.makeText(this, "Failed to load cart", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadMenuItems(List<String> menuItemIds, Map<String, Double> quantityMap) {
        db.collection("shops")
                .get()
                .addOnSuccessListener(shopsSnapshot -> {
                    for (DocumentSnapshot shop : shopsSnapshot.getDocuments()) {
                        String shopId = shop.getId();
                        String shopName = shop.getString("name");

                        db.collection("shops")
                                .document(shopId)
                                .collection("menu")
                                .get()
                                .addOnSuccessListener(menuSnapshot -> {
                                    for (DocumentSnapshot menuItem : menuSnapshot.getDocuments()) {
                                        if (menuItemIds.contains(menuItem.getId())) {
                                            displayCartItem(menuItem, shopName, quantityMap.get(menuItem.getId()));
                                        }
                                    }
                                    showLoading(false);
                                    updateOrderSummary();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error loading menu items", e);
                });
    }

    private void displayCartItem(DocumentSnapshot document, String shopName, Double quantity) {
        String itemName = document.getString("name");
        Double itemPrice = document.getDouble("price");
        String imageUrl = document.getString("imageUrl");

        if (itemName == null || itemPrice == null || quantity == null) {
            return;
        }

        double subtotal = itemPrice * quantity;
        totalPrice += subtotal;

        ItemCartBinding cartBinding = ItemCartBinding.inflate(getLayoutInflater());

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.image_placeholder)
                    .into(cartBinding.itemImage);
        }

        cartBinding.itemName.setText(itemName);
        cartBinding.itemPrice.setText(String.format("₱%.2f", itemPrice));
        cartBinding.itemShop.setText(shopName);
        cartBinding.quantity.setText(String.valueOf(quantity.intValue()));

        binding.itemContainer.addView(cartBinding.getRoot());
    }

    private void updateOrderSummary() {
        double subtotal = totalPrice;
        double deliveryFee = DELIVERY_FEE;
        double totalCost = subtotal + deliveryFee;

        binding.merchSubtotal.setText(String.format("Merchandise Subtotal: ₱%.2f", subtotal));
        binding.shipSubtotal.setText(String.format("Delivery Fee: ₱%.2f", deliveryFee));
        binding.totalPrice.setText(String.format("Total: ₱%.2f", totalCost));
    }

    private void handleGcashSelection() {
        selectedPaymentMethod = "GCash";
        showGcashQRDialog();
    }

    private void handleCodSelection() {
        selectedPaymentMethod = "Cash on Delivery";
        binding.proofContainer.setVisibility(View.GONE);
        hasPaidWithGcash = false;
        proofImageUri = null;
    }

    private void showGcashQRDialog() {
        DialogGcashQrBinding qrBinding = DialogGcashQrBinding.inflate(getLayoutInflater());

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(qrBinding.getRoot())
                .setTitle("GCash Payment")
                .setMessage("Scan the QR code to complete payment. After paying, click 'I've Paid' to upload proof.")
                .setCancelable(true)
                .create();

        qrBinding.btnConfirmPayment.setOnClickListener(v -> {
            hasPaidWithGcash = true;
            binding.proofContainer.setVisibility(View.VISIBLE);
            dialog.dismiss();

            binding.main.post(() -> binding.main.smoothScrollTo(0, binding.proofContainer.getTop()));
            Toast.makeText(this, "Please upload your proof of payment", Toast.LENGTH_LONG).show();
        });

        qrBinding.btnCancel.setOnClickListener(v -> {
            if (!isFirstTimeBuyer) {
                binding.radioCod.setChecked(true);
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private void validateAndPlaceOrder() {
        if (selectedPaymentMethod.isEmpty()) {
            Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedPaymentMethod.equals("GCash")) {
            if (!hasPaidWithGcash) {
                Toast.makeText(this, "Please complete GCash payment first", Toast.LENGTH_SHORT).show();
                showGcashQRDialog();
                return;
            }

            if (proofImageUri == null) {
                Toast.makeText(this, "Please upload proof of payment", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        placeOrder();
    }

    private void placeOrder() {
        binding.placeOrder.setEnabled(false);
        showLoading(true);

        // Generate unique order ID and receipt number
        String orderId = db.collection("orders").document().getId();
        String receiptNumber = generateReceiptNumber();

        if (proofImageUri != null) {
            uploadProofAndCreateOrder(orderId, receiptNumber);
        } else {
            createOrder(orderId, receiptNumber, null);
        }
    }

    private String generateReceiptNumber() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String datePart = dateFormat.format(new Date());
        int randomPart = (int) (Math.random() * 9000) + 1000; // 4-digit random number
        return "RCPT-" + datePart + "-" + randomPart;
    }

    private void uploadProofAndCreateOrder(String orderId, String receiptNumber) {
        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference("payment_proofs/" + userId + "/" + orderId + ".jpg");

        storageRef.putFile(proofImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        storageRef.getDownloadUrl().addOnSuccessListener(uri ->
                                createOrder(orderId, receiptNumber, uri.toString())))
                .addOnFailureListener(e -> {
                    showLoading(false);
                    binding.placeOrder.setEnabled(true);
                    Log.e(TAG, "Upload failed", e);
                    Toast.makeText(this, "Failed to upload proof", Toast.LENGTH_SHORT).show();
                });
    }

    private void createOrder(String orderId, String receiptNumber, String proofImageUrl) {
        // Calculate total cost with delivery fee
        double subtotal = totalPrice;
        double deliveryFee = DELIVERY_FEE;
        double totalCost = subtotal + deliveryFee;

        // Create main order data
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", orderId);
        orderData.put("receiptNumber", receiptNumber);
        orderData.put("userId", userId);
        orderData.put("userName", userName);
        orderData.put("name", addressData.get("name"));
        orderData.put("address", addressData.get("address"));
        orderData.put("mobile", addressData.get("mobile"));
        orderData.put("subtotal", subtotal);  // ADD THIS: Store subtotal separately
        orderData.put("deliveryFee", deliveryFee);  // ADD THIS: Store delivery fee
        orderData.put("totalPrice", totalCost);  // This should be subtotal + deliveryFee
        orderData.put("orderTimestamp", FieldValue.serverTimestamp());
        orderData.put("status", "Pending");
        orderData.put("paymentMethod", selectedPaymentMethod);
        orderData.put("timestamp", System.currentTimeMillis());
        orderData.put("orderType", isBuyNowFlow ? "buy_now" : "cart");

        if (proofImageUrl != null) {
            orderData.put("proofImageUrl", proofImageUrl);
        }

        Log.d(TAG, "Creating order with ID: " + orderId);
        Log.d(TAG, "Order Type: " + (isBuyNowFlow ? "Buy Now" : "Cart"));
        Log.d(TAG, "Subtotal: " + subtotal);
        Log.d(TAG, "Delivery Fee: " + deliveryFee);
        Log.d(TAG, "Total Price: " + totalCost);

        // Save order to Firestore
        db.collection("orders")
                .document(orderId)
                .set(orderData)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Order saved successfully to Firestore");
                    saveOrderItems(orderId);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    binding.placeOrder.setEnabled(true);
                    Log.e(TAG, "Failed to save order", e);
                    Toast.makeText(this, "Failed to save order", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveOrderItems(String orderId) {
        if (isBuyNowFlow) {
            // NEW: Save single item for Buy Now
            saveBuyNowItem(orderId);
        } else {
            // Save cart items
            if (cartItems.isEmpty()) {
                Log.e(TAG, "No cart items to save");
                clearCart(orderId);
                return;
            }

            // Save each cart item as a subcollection
            for (Map<String, Object> item : cartItems) {
                db.collection("orders")
                        .document(orderId)
                        .collection("items")
                        .add(item)
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to save order item", e);
                        });
            }

            // Also save to Realtime Database for admin panel
            saveToRealtimeDatabase(orderId);
        }
    }

    // NEW: Method to save single Buy Now item
    private void saveBuyNowItem(String orderId) {
        Map<String, Object> itemData = new HashMap<>();
        itemData.put("docId", itemDocId);
        itemData.put("shopId", shopId);
        itemData.put("quantity", quantity);
        itemData.put("itemName", itemName);
        itemData.put("itemPrice", itemPrice);

        db.collection("orders")
                .document(orderId)
                .collection("items")
                .add(itemData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Buy Now item saved successfully");
                    saveToRealtimeDatabase(orderId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save Buy Now item", e);
                    // Still proceed to save to Realtime DB
                    saveToRealtimeDatabase(orderId);
                });
    }

    private void saveToRealtimeDatabase(String orderId) {
        DatabaseReference orderRef = realtimeDb.getReference("orders").child(orderId);

        double subtotal = totalPrice;
        double deliveryFee = DELIVERY_FEE;
        double totalCost = subtotal + deliveryFee;

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", orderId);
        orderData.put("userId", userId);
        orderData.put("userName", userName);
        orderData.put("name", addressData.get("name"));
        orderData.put("address", addressData.get("address"));
        orderData.put("mobile", addressData.get("mobile"));
        orderData.put("subtotal", subtotal);  // ADD THIS
        orderData.put("deliveryFee", deliveryFee);  // ADD THIS
        orderData.put("totalPrice", totalCost);  // This should be subtotal + deliveryFee
        orderData.put("orderTimestamp", System.currentTimeMillis());
        orderData.put("status", "Pending");
        orderData.put("paymentMethod", selectedPaymentMethod);
        orderData.put("receiptNumber", generateReceiptNumber());
        orderData.put("orderType", isBuyNowFlow ? "buy_now" : "cart");

        orderRef.setValue(orderData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Order saved to Realtime Database");
                    if (isBuyNowFlow) {
                        // No cart to clear for Buy Now
                        createAdminNotification(orderId);
                    } else {
                        clearCart(orderId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save to Realtime Database", e);
                    if (isBuyNowFlow) {
                        createAdminNotification(orderId);
                    } else {
                        clearCart(orderId);
                    }
                });
    }

    private void clearCart(String orderId) {
        db.collection("users")
                .document(userId)
                .collection("cart")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        Log.d(TAG, "Cart already empty");
                        createAdminNotification(orderId);
                        return;
                    }

                    // Delete all cart items
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        doc.getReference().delete();
                    }

                    Log.d(TAG, "Cart cleared successfully");
                    createAdminNotification(orderId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to clear cart", e);
                    // Still proceed to create notification
                    createAdminNotification(orderId);
                });
    }

    private void createAdminNotification(String orderId) {
        // Create notification in Realtime Database
        DatabaseReference notificationsRef = realtimeDb.getReference("admin_notifications");
        String notificationId = notificationsRef.push().getKey();

        Map<String, Object> notification = new HashMap<>();
        notification.put("orderId", orderId);
        notification.put("type", "new_order");
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);
        notification.put("userId", userId);
        notification.put("userName", userName);
        notification.put("name", addressData.get("name"));
        notification.put("totalPrice", totalPrice + DELIVERY_FEE);
        notification.put("status", "Pending");
        notification.put("orderType", isBuyNowFlow ? "buy_now" : "cart");

        if (notificationId != null) {
            notificationsRef.child(notificationId).setValue(notification)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Admin notification created in Realtime DB");
                        // Also create in Firestore for backup
                        createFirestoreNotification(orderId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to create notification in Realtime DB", e);
                        createFirestoreNotification(orderId);
                    });
        } else {
            createFirestoreNotification(orderId);
        }
    }

    private void createFirestoreNotification(String orderId) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("orderId", orderId);
        notification.put("type", "new_order");
        notification.put("timestamp", FieldValue.serverTimestamp());
        notification.put("read", false);
        notification.put("userId", userId);
        notification.put("userName", userName);
        notification.put("name", addressData.get("name"));
        notification.put("totalPrice", totalPrice + DELIVERY_FEE);
        notification.put("status", "Pending");
        notification.put("orderType", isBuyNowFlow ? "buy_now" : "cart");

        db.collection("admin_notifications")
                .add(notification)
                .addOnSuccessListener(doc -> {
                    Log.d(TAG, "Admin notification created in Firestore");
                    navigateToSuccess(orderId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create notification in Firestore", e);
                    // Still navigate to success
                    navigateToSuccess(orderId);
                });
    }

    private void navigateToSuccess(String orderId) {
        showLoading(false);

        // Get receipt number for success screen
        String receiptNumber = generateReceiptNumber();

        Intent intent = new Intent(this, OrderSuccessActivity.class);
        intent.putExtra("orderId", orderId);
        intent.putExtra("receiptNumber", receiptNumber);
        intent.putExtra("totalAmount", totalPrice + DELIVERY_FEE);
        intent.putExtra("orderType", isBuyNowFlow ? "buy_now" : "cart");
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.placeOrder.setEnabled(!show);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            proofImageUri = data.getData();
            binding.imgProof.setImageURI(proofImageUri);
            Toast.makeText(this, "Proof uploaded successfully", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up
        if (binding != null) {
            binding = null;
        }
    }
}