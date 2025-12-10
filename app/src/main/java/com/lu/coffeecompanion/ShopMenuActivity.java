package com.lu.coffeecompanion;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lu.coffeecompanion.databinding.ActivityShopMenuBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ShopMenuActivity extends AppCompatActivity {

    ActivityShopMenuBinding binding;

    FirebaseFirestore db;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    LinearLayout mainContainer;
    String selectedCategory = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityShopMenuBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        String userId = null;

        //check if user is not logged in
        if(currentUser == null){
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
        //remove as comment if required
        /*
        else{
            userId = currentUser.getUid();
        }
        */

        //fetch documentId
        Intent intent = getIntent();
        String documentId = intent.getStringExtra("documentId");

        db.collection("shops")
                .document(documentId)
                        .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String shopName = document.getString("name");
                            if (shopName != null) {
                                binding.shopName.setText(shopName);
                            }
                        }
                    }
                });

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedCategory = tab.getText().toString();
                fetchMenu(documentId, selectedCategory);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        fetchCategories(documentId);
        fetchMenu(documentId, selectedCategory);

        binding.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchCategories(documentId);
                fetchMenu(documentId, selectedCategory);
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        });

        binding.back.setOnClickListener(v -> {
            finish();
        });
    }

    //generate menu from firebase
    public void fetchMenu(String documentId, String selectedCategory){
        mainContainer = findViewById(R.id.mainContainer);
        mainContainer.removeAllViews();

        binding.progressBar.setVisibility(View.VISIBLE);

        db.collection("shops")
                .document(documentId)
                .collection("menu")
                .orderBy("price")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        mainContainer.removeAllViews();
                        for (DocumentSnapshot document : task.getResult()) {
                            String itemName = (String) document.get("name");
                            double itemPrice = document.getDouble("price");
                            String itemDescription = (String) document.get("description");
                            String imageUrl = (String) document.get("imageUrl");
                            String category = (String) document.get("category");
                            String docId = document.getId();

                            View itemView = getLayoutInflater().inflate(R.layout.item_horizontalmenu, null);
                            TextView nameTextView = itemView.findViewById(R.id.itemName);
                            TextView priceTextView = itemView.findViewById(R.id.itemPrice);
                            TextView descriptionTextView = itemView.findViewById(R.id.itemDescription);
                            ImageView imageView = itemView.findViewById(R.id.imageView);
                            ImageButton plusButton = itemView.findViewById(R.id.plusButton);

                            LinearLayout itemContainer = itemView.findViewById(R.id.itemContainer);

                            if(category.equals(selectedCategory)){
                                nameTextView.setText(itemName);
                                priceTextView.setText("₱" + String.format("%.2f", itemPrice));
                                descriptionTextView.setText(itemDescription);
                                Glide.with(getApplicationContext()).load(imageUrl).into(imageView);
                                mainContainer.addView(itemView);

                                itemContainer.setOnClickListener(v -> {
                                    Intent intent = new Intent(getApplicationContext(), ItemActivity.class);
                                    intent.putExtra("docId", docId);
                                    intent.putExtra("documentId", documentId);
                                    startActivity(intent);
                                });
                                plusButton.setOnClickListener(v -> {
                                    Intent intent = new Intent(getApplicationContext(), ItemActivity.class);
                                    intent.putExtra("docId", docId);
                                    intent.putExtra("documentId", documentId);
                                    startActivity(intent);
                                });

                            }
                            binding.progressBar.setVisibility(View.GONE);

                        }
                    }
                    else{
                        binding.progressBar.setVisibility(View.GONE);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        binding.progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void fetchCategories(String documentId){
        binding.tabLayout.removeAllTabs();
        db.collection("shops")
                .document(documentId)
                .collection("menu")
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        List<String> categoryList = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            String category = document.getString("category");

                            if(!categoryList.contains(category)){
                                categoryList.add(category);
                                binding.tabLayout.addTab(binding.tabLayout.newTab().setText(category));
                            }

                        }
                    }
                });
    }
}