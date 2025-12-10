package com.lu.coffeecompanion;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.lu.coffeecompanion.databinding.ActivityInitializeUserDetailsBinding;
import com.lu.coffeecompanion.databinding.DialogEdituserdetailsBinding;
import com.lu.coffeecompanion.databinding.DialogWarningBinding;

import java.util.HashMap;
import java.util.Map;

public class EditAddressActivity extends AppCompatActivity {
    ActivityInitializeUserDetailsBinding binding;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseAuth auth = FirebaseAuth.getInstance();
    FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityInitializeUserDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUser = auth.getCurrentUser();
        String userId = currentUser.getUid();

        Intent getIntent = getIntent();
        String documentId = getIntent.getStringExtra("documentId");

        binding.titleText.setText("Edit Address");
        binding.btnDelete.setVisibility(View.VISIBLE);

        if (documentId != null) {
            fetchAddressDetails(userId, documentId);
        }

        //delete button with dialog popup
        binding.btnDelete.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            DialogWarningBinding Dbinding = DialogWarningBinding.inflate(getLayoutInflater());
            builder.setView(Dbinding.getRoot());
            AlertDialog dialog = builder.create();

            Dbinding.btnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            Dbinding.btnSubmit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    db.collection("users")
                            .document(userId)
                            .collection("addresses")
                            .document(documentId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                dialog.dismiss();
                                Toast.makeText(getApplicationContext(), "Address deleted successfully.", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                dialog.dismiss();
                                Toast.makeText(getApplicationContext(), "Unexpected error.", Toast.LENGTH_SHORT).show();
                            });
                }
            });

            dialog.show();
        });

        binding.btnSubmit.setOnClickListener(v -> {
            String name = binding.inputName.getText().toString().trim(),
                    address = binding.inputAddress.getText().toString().trim(),
                    mobile = binding.inputMobile.getText().toString().trim();

            binding.progressBar.setVisibility(View.VISIBLE);

            if(name.isEmpty()){
                binding.inputName.setError("Name cannot be empty.");
                binding.inputName.requestFocus();
                binding.progressBar.setVisibility(View.GONE);
            } else if (mobile.isEmpty()) {
                binding.inputMobile.setError("Mobile number cannot be empty.");
                binding.inputMobile.requestFocus();
                binding.progressBar.setVisibility(View.GONE);
            }
            else if (address.isEmpty()) {
                binding.inputAddress.setError("Address cannot be empty.");
                binding.inputAddress.requestFocus();
                binding.progressBar.setVisibility(View.GONE);
            }
            else if (mobile.length() < 10) {
                binding.inputMobile.setError("Mobile number should be at least 10 digits.");
                binding.inputMobile.requestFocus();
                binding.progressBar.setVisibility(View.GONE);
            }
            else if (!mobile.matches("[0-9]+")) {
                binding.inputMobile.setError("Mobile number should contain only digits.");
                binding.inputMobile.requestFocus();
                binding.progressBar.setVisibility(View.GONE);
            }
            else if (!name.matches("[a-zA-Z ]+")) {
                binding.inputName.setError("Name should contain only letters.");
                binding.inputName.requestFocus();
                binding.progressBar.setVisibility(View.GONE);
            }
            else if (address.length() < 5) {
                binding.inputAddress.setError("Address is too short.");
                binding.inputAddress.requestFocus();
                binding.progressBar.setVisibility(View.GONE);
            }
            else{
                updateAddress(userId, documentId, name, address, mobile);
            }

        });

    }
    private void fetchAddressDetails(String userId, String documentId) {
        db.collection("users").document(userId).collection("addresses").document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String address = documentSnapshot.getString("address");
                        String mobile = documentSnapshot.getString("mobile");

                        binding.inputName.setText(name);
                        binding.inputAddress.setText(address);
                        binding.inputMobile.setText(mobile);
                    }
                });
    }

    private void updateAddress(String userId, String documentId, String name, String address, String mobile) {
        Map<String, Object> updatedAddress = new HashMap<>();
        updatedAddress.put("name", name);
        updatedAddress.put("address", address);
        updatedAddress.put("mobile", mobile);

        db.collection("users").document(userId).collection("addresses").document(documentId)
                .update(updatedAddress)
                .addOnSuccessListener(aVoid -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getApplicationContext(), "Address edited successfully.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getApplicationContext(), AddressListActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });
    }
}