package com.lu.coffeecompanion;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lu.coffeecompanion.databinding.ActivityAdminAddCodPaymentBinding;

import java.util.HashMap;
import java.util.Map;

public class AdminAddCodPaymentActivity extends AppCompatActivity {

    ActivityAdminAddCodPaymentBinding binding;
    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminAddCodPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firestore = FirebaseFirestore.getInstance();

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnAddPayment.setOnClickListener(v -> {
            String customerName = binding.etCustomerName.getText().toString().trim();
            String amountStr = binding.etAmount.getText().toString().trim();
            String notes = binding.etNotes.getText().toString().trim();

            if (TextUtils.isEmpty(customerName)) {
                binding.etCustomerName.setError("Customer name is required");
                binding.etCustomerName.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(amountStr)) {
                binding.etAmount.setError("Amount is required");
                binding.etAmount.requestFocus();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    binding.etAmount.setError("Amount must be greater than 0");
                    binding.etAmount.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                binding.etAmount.setError("Invalid amount");
                binding.etAmount.requestFocus();
                return;
            }

            // Save COD payment to Firestore
            saveCodPayment(customerName, amount, notes);
        });
    }

    private void saveCodPayment(String customerName, double amount, String notes) {
        binding.btnAddPayment.setEnabled(false);
        binding.progressBar.setVisibility(android.view.View.VISIBLE);

        Map<String, Object> payment = new HashMap<>();
        payment.put("customerName", customerName);
        payment.put("amount", amount);
        payment.put("notes", notes);
        payment.put("paymentMethod", "Cash on Delivery");

        // 🔥 Use server timestamp (correct)
        payment.put("timestamp", FieldValue.serverTimestamp());

        firestore.collection("cod_payments")
                .add(payment)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "COD Payment added successfully!", Toast.LENGTH_SHORT).show();
                    binding.progressBar.setVisibility(android.view.View.GONE);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    binding.progressBar.setVisibility(android.view.View.GONE);
                    binding.btnAddPayment.setEnabled(true);
                });
    }
}
