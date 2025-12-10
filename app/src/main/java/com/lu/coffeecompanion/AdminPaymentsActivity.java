// AdminPaymentsActivity.java
package com.lu.coffeecompanion;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.lu.coffeecompanion.adapters.PaymentProofAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminPaymentsActivity extends AppCompatActivity implements PaymentProofAdapter.OnDeleteClickListener {

    RecyclerView recyclerPayments;
    ProgressBar progressBar;
    com.lu.coffeecompanion.adapters.PaymentProofAdapter adapter;
    List<Map<String, Object>> payments;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_payments);

        recyclerPayments = findViewById(R.id.recyclerPayments);
        progressBar = findViewById(R.id.progressBar);

        payments = new ArrayList<>();
        adapter = new com.lu.coffeecompanion.adapters.PaymentProofAdapter(payments, this);

        // Set the delete callback
        adapter.setDeleteClickListener(this::deletePayment);

        recyclerPayments.setLayoutManager(new LinearLayoutManager(this));
        recyclerPayments.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        fetchPayments();
    }

    private void fetchPayments() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("orders").orderBy("orderTimestamp")
                .get()
                .addOnSuccessListener((QuerySnapshot snapshot) -> {
                    payments.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Map<String, Object> payment = new HashMap<>();
                        payment.put("documentId", doc.getId()); // Important: Store document ID for deletion
                        payment.put("name", doc.getString("name") != null ? doc.getString("name") : "Unknown");
                        Object totalObj = doc.get("totalPrice");
                        payment.put("totalPrice", totalObj != null ? totalObj : 0.0);
                        payment.put("proofImageUrl", doc.getString("proofImageUrl") != null ? doc.getString("proofImageUrl") : "");

                        payments.add(payment);
                    }
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);

                    if (payments.isEmpty()) {
                        Toast.makeText(this, "No payments found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to fetch payments: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void deletePayment(int position) {
        if (position < 0 || position >= payments.size()) return;

        Map<String, Object> payment = payments.get(position);
        String documentId = (String) payment.get("documentId");
        String customerName = (String) payment.get("name");

        if (documentId == null || documentId.isEmpty()) {
            Toast.makeText(this, "Cannot delete: Invalid document", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Payment")
                .setMessage("Are you sure you want to delete the payment proof for " + customerName + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    performDelete(documentId, position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performDelete(String documentId, int position) {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("orders").document(documentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Remove from local list
                    payments.remove(position);
                    adapter.notifyItemRemoved(position);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Payment deleted successfully", Toast.LENGTH_SHORT).show();

                    // Check if list is empty after deletion
                    if (payments.isEmpty()) {
                        Toast.makeText(this, "No payments found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to delete payment: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onDeleteClick(int position) {

    }
}