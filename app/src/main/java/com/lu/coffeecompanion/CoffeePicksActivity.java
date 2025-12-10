package com.lu.coffeecompanion;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.lu.coffeecompanion.databinding.ActivityCoffeePicksBinding;

import java.util.ArrayList;
import java.util.List;

public class CoffeePicksActivity extends AppCompatActivity {

    ActivityCoffeePicksBinding binding;
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    String pickName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityCoffeePicksBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        Intent intent = getIntent();
        String docId = intent.getStringExtra("docId");

        if (docId != null) {
            db.collection("coffees").document(docId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    pickName = document.getString("name");
                    binding.title.setText(pickName);
                    // Instead of showing shops in tabs, directly fetch all menus
                    fetchAllMenuItems();
                }
            });
        } else {
            Toast.makeText(this, "Item fetching failed. Please try again.", Toast.LENGTH_SHORT).show();
        }

        // Hide the TabLayout since we don't need it anymore
        binding.tabLayout.setVisibility(View.GONE);

        binding.back.setOnClickListener(v -> {
            finish();
        });
    }

    private void fetchAllMenuItems() {
        binding.mainContainer.removeAllViews();
        binding.progressBar.setVisibility(View.VISIBLE);

        db.collection("shops").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                List<String> shopIds = new ArrayList<>();

                // First, collect all shop IDs
                for (QueryDocumentSnapshot shopDocument : querySnapshot) {
                    shopIds.add(shopDocument.getId());
                }

                // Counter to track how many shops have been processed
                final int[] processedCount = {0};
                final int totalShops = shopIds.size();

                if (totalShops == 0) {
                    binding.progressBar.setVisibility(View.GONE);
                    showNoItemsMessage();
                    return;
                }

                // Fetch menu items from each shop
                for (String shopId : shopIds) {
                    db.collection("shops").document(shopId).collection("menu")
                            .whereEqualTo("category", pickName)
                            .get()
                            .addOnCompleteListener(menuTask -> {
                                if (menuTask.isSuccessful()) {
                                    QuerySnapshot menuSnapshot = menuTask.getResult();
                                    for (QueryDocumentSnapshot menuDocument : menuSnapshot) {
                                        String category = menuDocument.getString("category");
                                        if (category != null && category.equals(pickName)) {
                                            displayMenuItem(menuDocument, shopId);
                                        }
                                    }
                                }

                                processedCount[0]++;
                                // Check if all shops have been processed
                                if (processedCount[0] == totalShops) {
                                    binding.progressBar.setVisibility(View.GONE);

                                    // Check if any items were added
                                    if (binding.mainContainer.getChildCount() == 0) {
                                        showNoItemsMessage();
                                    }
                                }
                            });
                }
            } else {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Failed to fetch shops", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayMenuItem(QueryDocumentSnapshot document, String shopId) {
        String itemName = document.getString("name");
        Double priceDouble = document.getDouble("price");
        String itemDescription = document.getString("description");
        String imageUrl = document.getString("imageUrl");
        String itemId = document.getId();

        // Handle null values
        double itemPrice = priceDouble != null ? priceDouble : 0.0;
        if (itemName == null) itemName = "Unnamed Item";
        if (itemDescription == null) itemDescription = "No description available";

        View itemView = getLayoutInflater().inflate(R.layout.item_horizontalmenu, null);
        TextView nameTextView = itemView.findViewById(R.id.itemName);
        TextView priceTextView = itemView.findViewById(R.id.itemPrice);
        TextView descriptionTextView = itemView.findViewById(R.id.itemDescription);
        ImageView imageView = itemView.findViewById(R.id.imageView);
        ImageButton plusButton = itemView.findViewById(R.id.plusButton);
        LinearLayout itemContainer = itemView.findViewById(R.id.itemContainer);

        nameTextView.setText(itemName);
        priceTextView.setText("₱" + String.format("%.2f", itemPrice));
        descriptionTextView.setText(itemDescription);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(getApplicationContext()).load(imageUrl).into(imageView);
        }

        binding.mainContainer.addView(itemView);

        itemContainer.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), ItemActivity.class);
            intent.putExtra("docId", itemId);
            intent.putExtra("documentId", shopId);
            startActivity(intent);
        });

        plusButton.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), ItemActivity.class);
            intent.putExtra("docId", itemId);
            intent.putExtra("documentId", shopId);
            startActivity(intent);
        });
    }

    private void showNoItemsMessage() {
        TextView noItemsText = new TextView(this);
        noItemsText.setText("No items found for this category.");
        noItemsText.setTextSize(16);
        noItemsText.setGravity(View.TEXT_ALIGNMENT_CENTER);
        noItemsText.setPadding(0, 50, 0, 0);
        binding.mainContainer.addView(noItemsText);
    }
}