package com.lu.coffeecompanion;

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
import com.lu.coffeecompanion.databinding.ActivityInitializeUserDetailsBinding;

import java.util.HashMap;
import java.util.Map;

public class AddAddressActivity extends AppCompatActivity {

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

        binding.titleText.setText("Add Address");

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
                if(currentUser != null){
                    Map<String, Object> addressDetails = new HashMap<>();
                    addressDetails.put("name", name);
                    addressDetails.put("address", address);
                    addressDetails.put("mobile", mobile);

                    db.collection("users").document(userId)
                            .collection("addresses")
                            .add(addressDetails)
                            .addOnSuccessListener(addressDocumentReference -> {
                                Toast.makeText(getApplicationContext(), "Address added successfully.", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                }
            }

        });

    }


}