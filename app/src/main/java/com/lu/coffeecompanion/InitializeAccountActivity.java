package com.lu.coffeecompanion;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.lu.coffeecompanion.databinding.ActivityInitializeAccountBinding;

import java.util.HashMap;
import java.util.Map;

public class InitializeAccountActivity extends AppCompatActivity {

    ActivityInitializeAccountBinding binding;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    FirebaseFirestore db;


    //user must not access this activity if not logged in
    @Override
    public void onStart() {
        super.onStart();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser == null){
            Intent intent = new Intent (getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_initialize_account);

        binding = ActivityInitializeAccountBinding.inflate(getLayoutInflater());
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
                                    if(document.contains("role")){
                                        String value = document.getString("role");
                                        if (!value.equals("unset")){
                                            Intent intent = new Intent(getApplicationContext(), InitializeUserDetailsActivity.class);
                                            startActivity(intent);
                                            finish();
                                        }

                                    }
                                }
                            }
                        });

        binding.btnCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUserRole("customer");
            }
        });

        binding.btnShop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUserRole("shop");
            }
        });

    }

    private void updateUserRole(String role) {
        if (currentUser != null) {
            String uid = currentUser.getUid();
            Map<String, Object> userData = new HashMap<>();
            userData.put("role", role);

            db.collection("users").document(uid)
                    .set(userData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Intent intent;
                        switch (role) {
                            case "shop":
                                intent = new Intent(getApplicationContext(), AdminDashboardActivity.class);
                                break;
                            case "customer":
                                intent = new Intent(getApplicationContext(), InitializeUserDetailsActivity.class);
                                break;
                            default:
                                Toast.makeText(getApplicationContext(), "Unexpected role: " + role, Toast.LENGTH_SHORT).show();
                                return;
                        }
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "No user signed in.", Toast.LENGTH_SHORT).show();
        }
    }
}