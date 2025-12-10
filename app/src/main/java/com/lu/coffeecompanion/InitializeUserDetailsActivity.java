package com.lu.coffeecompanion;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lu.coffeecompanion.databinding.ActivityInitializeUserDetailsBinding;

import java.util.HashMap;
import java.util.Map;

public class InitializeUserDetailsActivity extends AppCompatActivity {

    ActivityInitializeUserDetailsBinding binding;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_initialize_user_details);

        binding = ActivityInitializeUserDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        String userId = currentUser.getUid();
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        DocumentSnapshot document = task.getResult();
                        if(document.exists()){
                            if(document.contains("name")){
                                Intent intent = new Intent(getApplicationContext(), UserDashboardActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        }
                    }
                });

        binding.btnSubmit.setOnClickListener(v -> {
            binding.progressBar.setVisibility(View.VISIBLE);
            String name = binding.inputName.getText().toString().trim();
            String mobile = binding.inputMobile.getText().toString().trim();
            String address = binding.inputAddress.getText().toString().trim();

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
                if(currentUser != null){
                    Map<String, Object> userDetails = new HashMap<>();
                    userDetails.put("name", name);
                    userDetails.put("mobile", mobile);

                    db.collection("users").document(userId)
                            .update(userDetails)
                            .addOnSuccessListener(documentSnapshot -> {
                                Map<String, Object> addressDetails = new HashMap<>();
                                addressDetails.put("name", name);
                                addressDetails.put("address", address);
                                addressDetails.put("mobile", mobile);

                                db.collection("users").document(userId)
                                        .collection("addresses")
                                        .add(addressDetails)
                                        .addOnSuccessListener(addressDocumentReference -> {
                                            Intent intent = new Intent(getApplicationContext(), UserDashboardActivity.class);
                                            startActivity(intent);
                                            finish();
                                        });
                            });
                }
            }
        });

    }
}