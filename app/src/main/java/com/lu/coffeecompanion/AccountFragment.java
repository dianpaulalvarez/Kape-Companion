package com.lu.coffeecompanion;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.lu.coffeecompanion.databinding.AccountFragmentBinding;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AccountFragment extends Fragment {

    private static final String TAG = "AccountFragment";

    private AccountFragmentBinding binding;
    private DatabaseReference db;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private String userId;
    private ListenerRegistration orderListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = AccountFragmentBinding.inflate(inflater, container, false);

        initializeFirebase();

        if (!checkUserAuthentication()) {
            return binding.getRoot();
        }

        setupUI();
        loadUserData();

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (userId != null && binding != null) {
            loadRecentOrders();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (orderListener != null) {
            orderListener.remove();
            orderListener = null;
        }
        binding = null;
    }

    private void initializeFirebase() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference();
        firestore = FirebaseFirestore.getInstance();
    }

    private boolean checkUserAuthentication() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            navigateToLogin();
            return false;
        }
        userId = currentUser.getUid();
        return true;
    }

    private void navigateToLogin() {
        if (getContext() == null) return;
        Intent intent = new Intent(requireContext(), MainActivity.class);
        startActivity(intent);
        requireActivity().finish();
    }

    private void setupUI() {
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            loadUserData();
            loadRecentOrders();
            binding.swipeRefreshLayout.setRefreshing(false);
        });

        binding.btnName.setOnClickListener(v ->
                showEditDialog("Name", binding.name.getText().toString(), "name"));

        binding.btnEmail.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Email cannot be changed", Toast.LENGTH_SHORT).show());

        binding.btnMobile.setOnClickListener(v ->
                showEditDialog("Mobile Number", binding.mobile.getText().toString(), "mobile"));

        binding.addresses.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddressListActivity.class);
            startActivity(intent);
        });

        binding.orders.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), OrdersActivity.class);
            startActivity(intent);
        });

        binding.tvViewAllOrders.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), OrdersActivity.class);
            startActivity(intent);
        });

        binding.btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void loadUserData() {
        db.child("users").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (getContext() == null) return;

                        if (dataSnapshot.exists()) {
                            String name = dataSnapshot.child("name").getValue(String.class);
                            String email = dataSnapshot.child("email").getValue(String.class);
                            String mobile = dataSnapshot.child("mobile").getValue(String.class);
                            Boolean blocked = dataSnapshot.child("blocked").getValue(Boolean.class);

                            if (blocked != null && blocked) {
                                Toast.makeText(requireContext(), "Your account has been blocked", Toast.LENGTH_LONG).show();
                                auth.signOut();
                                navigateToLogin();
                                return;
                            }

                            if (binding != null) {
                                binding.textName.setText(name != null ? name : "User");
                                binding.name.setText(name != null ? name : "Not set");
                                binding.email.setText(email != null ? email : "Not set");
                                binding.mobile.setText(mobile != null ? mobile : "Not set");
                            }
                        } else {
                            createUserData();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Failed to load user data", databaseError.toException());
                    }
                });
    }

    private void loadRecentOrders() {
        if (binding == null || getContext() == null) return;

        // Remove old listener
        if (orderListener != null) {
            orderListener.remove();
        }

        binding.orderListContainer.removeAllViews();
        Log.d(TAG, "Loading orders for userId: " + userId);

        Query query = firestore.collection("orders")
                .whereEqualTo("userId", userId)
                .orderBy("orderTimestamp", Query.Direction.DESCENDING)
                .limit(5);

        orderListener = query.addSnapshotListener((queryDocumentSnapshots, error) -> {
            if (getContext() == null || binding == null) return;

            if (error != null) {
                Log.e(TAG, "Error loading orders", error);

                if (error.getMessage() != null && error.getMessage().contains("index")) {
                    loadOrdersWithoutIndex();
                    return;
                }

                binding.emptyOrderState.setVisibility(View.VISIBLE);
                binding.orderListContainer.setVisibility(View.GONE);
                return;
            }

            if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                Log.d(TAG, "No orders found for user: " + userId);
                binding.emptyOrderState.setVisibility(View.VISIBLE);
                binding.orderListContainer.setVisibility(View.GONE);
                return;
            }

            Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " orders");
            binding.emptyOrderState.setVisibility(View.GONE);
            binding.orderListContainer.setVisibility(View.VISIBLE);
            binding.orderListContainer.removeAllViews();

            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                addOrderCard(document);
            }
        });
    }

    private void loadOrdersWithoutIndex() {
        if (binding == null || getContext() == null) return;

        firestore.collection("orders")
                .orderBy("orderTimestamp", Query.Direction.DESCENDING)
                .limit(20)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (getContext() == null || binding == null) return;

                    if (error != null) {
                        Log.e(TAG, "Error loading all orders", error);
                        binding.emptyOrderState.setVisibility(View.VISIBLE);
                        binding.orderListContainer.setVisibility(View.GONE);
                        return;
                    }

                    if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                        binding.emptyOrderState.setVisibility(View.VISIBLE);
                        binding.orderListContainer.setVisibility(View.GONE);
                        return;
                    }

                    binding.emptyOrderState.setVisibility(View.GONE);
                    binding.orderListContainer.setVisibility(View.VISIBLE);
                    binding.orderListContainer.removeAllViews();

                    int userOrderCount = 0;
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String docUserId = document.getString("userId");
                        if (userId.equals(docUserId) && userOrderCount < 5) {
                            addOrderCard(document);
                            userOrderCount++;
                        }
                    }

                    if (userOrderCount == 0) {
                        binding.emptyOrderState.setVisibility(View.VISIBLE);
                        binding.orderListContainer.setVisibility(View.GONE);
                    }
                });
    }

    private void addOrderCard(QueryDocumentSnapshot document) {
        if (getContext() == null || binding == null) return;

        try {
            View orderCard = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_order_user_card, binding.orderListContainer, false);

            String orderId = document.getId();
            String receiptNumber = document.getString("receiptNumber");
            String status = document.getString("status");
            Double totalPrice = document.getDouble("totalPrice"); // TOTAL NA (subtotal + deliveryFee)
            Double deliveryFee = document.getDouble("deliveryFee");
            Double subtotal = document.getDouble("subtotal");
            com.google.firebase.Timestamp timestamp = document.getTimestamp("orderTimestamp");

            TextView tvReceiptNumber = orderCard.findViewById(R.id.tvReceiptNumber);
            TextView tvOrderDate = orderCard.findViewById(R.id.tvOrderDate);
            TextView tvTotalAmount = orderCard.findViewById(R.id.tvTotalAmount);
            TextView tvStatus = orderCard.findViewById(R.id.tvStatus);

            if (tvReceiptNumber != null) {
                tvReceiptNumber.setText(receiptNumber != null ? receiptNumber :
                        "Order #" + (orderId.length() > 8 ? orderId.substring(0, 8) : orderId).toUpperCase());
            }

            if (tvOrderDate != null && timestamp != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                tvOrderDate.setText(sdf.format(timestamp.toDate()));
            }

            if (tvTotalAmount != null) {
                // FIXED: Directly use totalPrice since it already includes delivery fee
                double total = totalPrice != null ? totalPrice : 0.0;

                // For backward compatibility with old orders
                if (total == 0.0 && subtotal != null && deliveryFee != null) {
                    total = subtotal + deliveryFee;
                }

                tvTotalAmount.setText(String.format(Locale.getDefault(), "₱%.2f", total));
            }

            if (tvStatus != null) {
                tvStatus.setText(status != null ? status.toUpperCase() : "PENDING");
                setStatusStyle(tvStatus, status);
            }

            orderCard.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), OrderDetailsActivity.class);
                intent.putExtra("orderId", orderId);
                startActivity(intent);
            });

            binding.orderListContainer.addView(orderCard);

        } catch (Exception e) {
            Log.e(TAG, "Error adding order card", e);
        }
    }

    private void setStatusStyle(TextView tvStatus, String status) {
        if (status == null) status = "pending";

        int backgroundColor;
        switch (status.toLowerCase()) {
            case "pending":
                backgroundColor = 0xFFFFC107; // Yellow
                break;
            case "preparing":
            case "confirmed":
                backgroundColor = 0xFF2196F3; // Blue
                break;
            case "out for delivery":
                backgroundColor = 0xFFFF9800; // Orange
                break;
            case "delivered":
            case "completed":
                backgroundColor = 0xFF4CAF50; // Green
                break;
            case "cancelled":
                backgroundColor = 0xFFF44336; // Red
                break;
            default:
                backgroundColor = 0xFFFFC107;
        }

        tvStatus.setBackgroundColor(backgroundColor);
        tvStatus.setTextColor(0xFFFFFFFF);
        tvStatus.setPadding(24, 12, 24, 12);
    }

    private void createUserData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User");
        userData.put("email", currentUser.getEmail());
        userData.put("mobile", "Not set");
        userData.put("role", "user");
        userData.put("blocked", false);

        db.child("users").child(userId)
                .setValue(userData)
                .addOnSuccessListener(aVoid -> loadUserData())
                .addOnFailureListener(e -> Log.e(TAG, "Failed to create user data", e));
    }

    private void showEditDialog(String title, String currentValue, String field) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit " + title);

        final EditText input = new EditText(requireContext());
        input.setText(currentValue);
        input.setPadding(32, 24, 32, 24);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newValue = input.getText().toString().trim();

            if (newValue.isEmpty()) {
                Toast.makeText(requireContext(), title + " cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            if (field.equals("mobile") && newValue.length() < 10) {
                Toast.makeText(requireContext(), "Invalid mobile number", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put(field, newValue);

            db.child("users").child(userId)
                    .updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        if (getContext() != null) {
                            Toast.makeText(requireContext(), title + " updated successfully", Toast.LENGTH_SHORT).show();
                            loadUserData();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (getContext() != null) {
                            Toast.makeText(requireContext(), "Failed to update " + title, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showLogoutDialog() {
        if (getContext() == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    auth.signOut();
                    navigateToLogin();
                })
                .setNegativeButton("No", null)
                .show();
    }
}