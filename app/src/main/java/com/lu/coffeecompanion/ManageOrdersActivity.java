package com.lu.coffeecompanion;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.lu.coffeecompanion.databinding.ItemOrderAdminBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ManageOrdersActivity extends AppCompatActivity {

    // Remove ViewBinding since we don't have the XML file yet
    private FirebaseFirestore db;
    private List<Order> orderList;
    private OrderAdapter adapter;

    // UI elements
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvOrders;
    private LinearLayout emptyState;
    private ProgressBar progressBar;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manage_orders); // Use direct layout reference

        // Initialize UI elements
        toolbar = findViewById(R.id.toolbar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        rvOrders = findViewById(R.id.rvOrders);
        emptyState = findViewById(R.id.emptyState);
        progressBar = findViewById(R.id.progressBar);

        db = FirebaseFirestore.getInstance();
        orderList = new ArrayList<>();

        setupUI();
        loadOrders();
    }

    private void setupUI() {
        toolbar.setNavigationOnClickListener(v -> finish());

        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadOrders();
            swipeRefreshLayout.setRefreshing(false);
        });

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter();
        rvOrders.setAdapter(adapter);
    }

    private void loadOrders() {
        progressBar.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        rvOrders.setVisibility(View.GONE);

        db.collection("orders")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    orderList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Order order = new Order();
                        order.id = document.getId();
                        order.userId = document.getString("userId");
                        order.userName = document.getString("userName");
                        order.customerName = document.getString("name");
                        order.address = document.getString("address");
                        order.mobile = document.getString("mobile");
                        order.totalPrice = document.getDouble("totalPrice");
                        order.deliveryFee = document.getDouble("deliveryFee");
                        order.paymentMethod = document.getString("paymentMethod");
                        order.status = document.getString("status");
                        order.timestamp = document.getLong("timestamp");

                        if (document.getTimestamp("orderTimestamp") != null) {
                            order.orderDate = document.getTimestamp("orderTimestamp").toDate();
                        }

                        orderList.add(order);
                    }

                    if (orderList.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                        rvOrders.setVisibility(View.GONE);
                    } else {
                        emptyState.setVisibility(View.GONE);
                        rvOrders.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }

                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load orders", Toast.LENGTH_SHORT).show();
                });
    }

    private class Order {
        String id;
        String userId;
        String userName;
        String customerName;
        String address;
        String mobile;
        Double totalPrice;
        Double deliveryFee;
        String paymentMethod;
        String status;
        Long timestamp;
        Date orderDate;
    }

    private class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

        @NonNull
        @Override
        public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemOrderAdminBinding binding = ItemOrderAdminBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new OrderViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
            Order order = orderList.get(position);
            holder.bind(order);
        }

        @Override
        public int getItemCount() {
            return orderList.size();
        }

        class OrderViewHolder extends RecyclerView.ViewHolder {
            ItemOrderAdminBinding binding;

            OrderViewHolder(ItemOrderAdminBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            void bind(Order order) {
                binding.tvOrderId.setText("Order #" + order.id.substring(0, 8).toUpperCase());
                binding.tvCustomerName.setText(order.customerName != null ? order.customerName : "Unknown");
                binding.tvPaymentMethod.setText(order.paymentMethod != null ? order.paymentMethod : "Unknown");
                binding.tvStatus.setText(order.status != null ? order.status.toUpperCase() : "PENDING");

                double total = (order.totalPrice != null ? order.totalPrice : 0.0) +
                        (order.deliveryFee != null ? order.deliveryFee : 0.0);
                binding.tvTotal.setText(String.format("₱%.2f", total));

                if (order.orderDate != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                    binding.tvOrderDate.setText(sdf.format(order.orderDate));
                } else if (order.timestamp != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                    binding.tvOrderDate.setText(sdf.format(new Date(order.timestamp)));
                }

                setStatusColor(binding.tvStatus, order.status);

                loadOrderItems(order.id);

                setupActionButtons(order);

                binding.cardOrder.setOnClickListener(v -> {
                    Intent intent = new Intent(ManageOrdersActivity.this, OrderDetailsActivity.class);
                    intent.putExtra("orderId", order.id);
                    intent.putExtra("adminView", true);
                    startActivity(intent);
                });
            }

            private void loadOrderItems(String orderId) {
                db.collection("orders").document(orderId)
                        .collection("items")
                        .limit(3)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            StringBuilder summary = new StringBuilder("Items: ");
                            int count = 0;

                            for (QueryDocumentSnapshot itemDoc : queryDocumentSnapshots) {
                                String docId = itemDoc.getString("docId");
                                Long quantity = itemDoc.getLong("quantity");

                                if (count > 0) summary.append(", ");
                                summary.append(quantity != null ? quantity + "x " : "1x ");

                                if (docId != null) {
                                    fetchMenuItemName(docId, summary, count);
                                }

                                count++;
                            }

                            if (count > 3) {
                                summary.append(" and more...");
                            }

                            binding.tvItems.setText(summary.toString());
                        })
                        .addOnFailureListener(e -> {
                            binding.tvItems.setText("Items: Loading...");
                        });
            }

            private void fetchMenuItemName(String menuItemId, StringBuilder summary, int position) {
                db.collection("shops")
                        .get()
                        .addOnSuccessListener(shopsSnapshot -> {
                            for (DocumentSnapshot shop : shopsSnapshot.getDocuments()) {
                                db.collection("shops").document(shop.getId())
                                        .collection("menu").document(menuItemId)
                                        .get()
                                        .addOnSuccessListener(menuItem -> {
                                            if (menuItem.exists()) {
                                                String itemName = menuItem.getString("name");
                                                if (itemName != null) {
                                                    String currentText = summary.toString();
                                                    String[] parts = currentText.split(": ");
                                                    if (parts.length > 1) {
                                                        String itemsText = parts[1];
                                                        String[] items = itemsText.split(", ");
                                                        if (items.length > position) {
                                                            items[position] = items[position].split("x ")[0] + "x " + itemName;
                                                            String newText = "Items: " + TextUtils.join(", ", items);
                                                            summary.replace(0, summary.length(), newText);
                                                            binding.tvItems.setText(newText);
                                                        }
                                                    }
                                                }
                                            }
                                        });
                            }
                        });
            }

            private void setStatusColor(TextView tvStatus, String status) {
                if (status == null) {
                    tvStatus.setBackgroundResource(R.drawable.status_background_pending);
                    return;
                }

                switch (status.toLowerCase()) {
                    case "pending":
                        tvStatus.setBackgroundResource(R.drawable.status_background_pending);
                        tvStatus.setTextColor(getResources().getColor(android.R.color.white));
                        break;
                    case "preparing":
                        tvStatus.setBackgroundResource(R.drawable.status_background_preparing);
                        tvStatus.setTextColor(getResources().getColor(android.R.color.white));
                        break;
                    case "out for delivery":
                        tvStatus.setBackgroundResource(R.drawable.status_background_delivery);
                        tvStatus.setTextColor(getResources().getColor(android.R.color.white));
                        break;
                    case "completed":
                        tvStatus.setBackgroundResource(R.drawable.status_background_completed);
                        tvStatus.setTextColor(getResources().getColor(android.R.color.white));
                        break;
                    default:
                        tvStatus.setBackgroundResource(R.drawable.status_background_pending);
                        tvStatus.setTextColor(getResources().getColor(android.R.color.white));
                }
            }

            private void setupActionButtons(Order order) {
                binding.btnPreparing.setOnClickListener(v -> updateOrderStatus(order.id, "Preparing"));
                binding.btnDelivery.setOnClickListener(v -> updateOrderStatus(order.id, "Out for Delivery"));
                binding.btnCompleted.setOnClickListener(v -> updateOrderStatus(order.id, "Completed"));

                updateButtonVisibility(order.status);
            }

            private void updateButtonVisibility(String status) {
                if (status == null) {
                    binding.btnPreparing.setVisibility(View.VISIBLE);
                    binding.btnDelivery.setVisibility(View.GONE);
                    binding.btnCompleted.setVisibility(View.GONE);
                    return;
                }

                switch (status.toLowerCase()) {
                    case "pending":
                        binding.btnPreparing.setVisibility(View.VISIBLE);
                        binding.btnDelivery.setVisibility(View.GONE);
                        binding.btnCompleted.setVisibility(View.GONE);
                        break;
                    case "preparing":
                        binding.btnPreparing.setVisibility(View.GONE);
                        binding.btnDelivery.setVisibility(View.VISIBLE);
                        binding.btnCompleted.setVisibility(View.GONE);
                        break;
                    case "out for delivery":
                        binding.btnPreparing.setVisibility(View.GONE);
                        binding.btnDelivery.setVisibility(View.GONE);
                        binding.btnCompleted.setVisibility(View.VISIBLE);
                        break;
                    case "completed":
                        binding.btnPreparing.setVisibility(View.GONE);
                        binding.btnDelivery.setVisibility(View.GONE);
                        binding.btnCompleted.setVisibility(View.GONE);
                        break;
                }
            }

            private void updateOrderStatus(String orderId, String newStatus) {
                db.collection("orders").document(orderId)
                        .update("status", newStatus)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(ManageOrdersActivity.this,
                                    "Order status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                            loadOrders();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(ManageOrdersActivity.this,
                                    "Failed to update status", Toast.LENGTH_SHORT).show();
                        });
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
    }
}