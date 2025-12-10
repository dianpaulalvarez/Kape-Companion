package com.lu.coffeecompanion;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.lu.coffeecompanion.databinding.ActivityItemBinding;

import java.util.HashMap;
import java.util.Map;

public class EditItemCartActivity extends AppCompatActivity {

    ActivityItemBinding binding;
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    String userId, currentItemId, forDelete;
    double quantityDouble;
    int counter = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityItemBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        if(currentUser == null){
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
        else{
            userId = currentUser.getUid();
        }

        binding.addToCart.setText("Save");

        Intent getIntent = getIntent();
        String cartItemId = getIntent.getStringExtra("cartItemId");
        String shopDocId = getIntent.getStringExtra("shopDocId");

        db.collection("users")
                .document(userId)
                .collection("cart")
                .whereEqualTo("docId", cartItemId)
                .get()
                .addOnCompleteListener(task2 -> {
                    if (task2.isSuccessful()) {
                        QuerySnapshot querySnapshot = task2.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                            forDelete = document.getId();
                            quantityDouble = document.getDouble("quantity");
                            binding.quantity.setText(String.format("%d", (int) quantityDouble));
                            currentItemId = document.getString("docId");
                            fetchShopItem(shopDocId);
                        }
                    }
                });

        binding.btnAdd.setOnClickListener(v -> {
            String counterString = binding.quantity.getText().toString().trim();
            counter = Integer.parseInt(counterString);
            counter++;
            binding.quantity.setText(String.valueOf(counter));
        });
        binding.btnMinus.setOnClickListener(v -> {
            String counterString = binding.quantity.getText().toString().trim();
            counter = Integer.parseInt(counterString);
            if(counter == 0){
                counter = 0;
            }
            else{
                counter--;
            }
            binding.quantity.setText(String.valueOf(counter));
        });

        binding.back.setOnClickListener(v -> {
            finish();
        });

        binding.addToCart.setOnClickListener(v -> {
            String counterString = binding.quantity.getText().toString().trim();
            counter = Integer.parseInt(counterString);

            Map<String, Object> cartItem = new HashMap<>();
            String currentCountString = binding.quantity.getText().toString().trim();
            int currentCount = Integer.parseInt(currentCountString);
            cartItem.put("docId", cartItemId);
            cartItem.put("quantity", currentCount);
            if(currentCount == 0){
                db.collection("users")
                        .document(userId)
                        .collection("cart")
                        .document(forDelete)
                        .delete().addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Item removed from cart.", Toast.LENGTH_SHORT).show();
                            finish();
                        });
            }
            else{
                db.collection("users")
                        .document(userId)
                        .collection("cart")
                        .whereEqualTo("docId", cartItemId)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                DocumentSnapshot document = task.getResult().getDocuments().get(0);
                                String existingDocId = document.getId();

                                db.collection("users")
                                        .document(userId)
                                        .collection("cart")
                                        .document(existingDocId)
                                        .update("quantity", currentCount)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, "Item quantity updated.", Toast.LENGTH_SHORT).show();
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Unexpected error.", Toast.LENGTH_SHORT).show();
                                        });
                            }
                        });
            }


        });

    }

    public void fetchShopItem(String shopDocId){
        db.collection("shops")
                .document(shopDocId)
                .collection("menu")
                .document(currentItemId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String itemName = (String) document.get("name");
                            double itemPrice = document.getDouble("price");
                            String itemDescription = (String) document.get("description");
                            String imageUrl = (String) document.get("imageUrl");

                            Glide.with(getApplicationContext()).load(imageUrl).into(binding.itemImage);
                            binding.itemImage.setTextDirection(Integer.parseInt(itemName));
                            binding.itemTitle.setText(itemName);
                            binding.itemPrice.setText("₱" + String.format("%.2f", itemPrice));
                            binding.itemDescription.setText(itemDescription);


                        }
                    }
                });
    }


}