package com.lu.coffeecompanion;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.lu.coffeecompanion.adapters.OrderAdapter;
import com.lu.coffeecompanion.models.Order;

import java.util.ArrayList;
import java.util.Collections;

public class AdminOrdersActivity extends AppCompatActivity {

    private RecyclerView recyclerOrders;
    private OrderAdapter adapter;
    private ArrayList<Order> orderList;
    private DatabaseReference ordersRef;
    private ProgressBar progressBar;
    private ImageView btnBack;
    private TextView tvNoOrders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_orders);

        // Initialize views
        recyclerOrders = findViewById(R.id.recyclerOrders);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);
        tvNoOrders = findViewById(R.id.tvNoOrders);

        // Setup RecyclerView
        recyclerOrders.setLayoutManager(new LinearLayoutManager(this));
        recyclerOrders.setHasFixedSize(true);

        // Initialize order list and adapter
        orderList = new ArrayList<>();
        adapter = new OrderAdapter(this, orderList);
        recyclerOrders.setAdapter(adapter);

        // Initialize Firebase Database Reference
        ordersRef = FirebaseDatabase.getInstance().getReference("orders");

        // Setup back button
        btnBack.setOnClickListener(v -> finish());

        // Load orders from Firebase
        loadOrders();
    }

    private void loadOrders() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerOrders.setVisibility(View.GONE);
        tvNoOrders.setVisibility(View.GONE);

        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                orderList.clear();

                if (snapshot.exists()) {
                    for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                        try {
                            Order order = orderSnapshot.getValue(Order.class);
                            if (order != null) {
                                order.setOrderId(orderSnapshot.getKey());
                                orderList.add(order);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    // Sort orders by timestamp (newest first)
                    Collections.sort(orderList, (o1, o2) ->
                            Long.compare(o2.getTimestamp(), o1.getTimestamp()));

                    adapter.notifyDataSetChanged();
                    recyclerOrders.setVisibility(View.VISIBLE);
                    tvNoOrders.setVisibility(View.GONE);
                } else {
                    tvNoOrders.setVisibility(View.VISIBLE);
                    recyclerOrders.setVisibility(View.GONE);
                }

                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminOrdersActivity.this,
                        "Failed to load orders: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                tvNoOrders.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove Firebase listener to prevent memory leaks
        if (ordersRef != null) {
            ordersRef.removeEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {}
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }
}