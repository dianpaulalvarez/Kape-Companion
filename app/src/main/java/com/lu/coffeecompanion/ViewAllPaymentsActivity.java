package com.lu.coffeecompanion;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.lu.coffeecompanion.adapters.PaymentAdapter;
import com.lu.coffeecompanion.databinding.ActivityViewAllPaymentsBinding;
import com.lu.coffeecompanion.models.Payment;

import java.util.ArrayList;
import java.util.List;

public class ViewAllPaymentsActivity extends AppCompatActivity {

    private static final String TAG = "ViewAllPayments";

    private ActivityViewAllPaymentsBinding binding;
    private FirebaseFirestore db;
    private PaymentAdapter adapter;
    private List<Payment> paymentList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityViewAllPaymentsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeFirebase();
        setupUI();
        loadPayments();
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
    }

    private void setupUI() {
        binding.back.setOnClickListener(v -> finish());

        paymentList = new ArrayList<>();
        adapter = new PaymentAdapter(paymentList, this::onPaymentClick);

        binding.recyclerViewPayments.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewPayments.setAdapter(adapter);
    }

    private void loadPayments() {
        showLoading(true);
        showEmptyState(false);

        db.collection("orders")
                .orderBy("orderTimestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    paymentList.clear();

                    if (querySnapshot.isEmpty()) {
                        showLoading(false);
                        showEmptyState(true);
                        return;
                    }

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Payment payment = new Payment();
                        payment.setOrderId(doc.getId());
                        payment.setReceiptNumber(doc.getString("receiptNumber"));
                        payment.setCustomerName(doc.getString("name"));
                        payment.setContactNumber(doc.getString("mobile"));
                        payment.setAddress(doc.getString("address"));
                        payment.setPaymentMethod(doc.getString("paymentMethod"));

                        Double totalPrice = doc.getDouble("totalPrice");
                        Double deliveryFee = doc.getDouble("deliveryFee");

                        payment.setTotalAmount(totalPrice != null ? totalPrice : 0.0);
                        payment.setDeliveryFee(deliveryFee != null ? deliveryFee : 0.0);
                        payment.setOrderTimestamp(doc.getTimestamp("orderTimestamp"));
                        payment.setProofImageUrl(doc.getString("proofImageUrl"));
                        payment.setStatus(doc.getString("status"));

                        paymentList.add(payment);
                    }

                    adapter.notifyDataSetChanged();
                    showLoading(false);
                    showEmptyState(paymentList.isEmpty());
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showEmptyState(true);
                    Log.e(TAG, "Error loading payments", e);
                    Toast.makeText(this, "Failed to load payments", Toast.LENGTH_SHORT).show();
                });
    }

    private void onPaymentClick(Payment payment) {
        Intent intent = new Intent(this, ViewReceiptActivity.class);
        intent.putExtra("orderId", payment.getOrderId());
        startActivity(intent);
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.recyclerViewPayments.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showEmptyState(boolean show) {
        binding.emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.recyclerViewPayments.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}