package com.lu.coffeecompanion;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Typeface;
import androidx.core.content.ContextCompat;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryManagementActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private RecyclerView recyclerView;
    private InventoryAdapter adapter;
    private List<InventoryItem> inventoryList;
    private TextView tvEmptyState;
    private static final int PICK_IMAGE_REQUEST = 1001;
    private Bitmap selectedImageBitmap;
    private ImageView currentDialogImageView;
    private androidx.appcompat.app.AlertDialog currentAddDialog;
    private Bitmap dialogSelectedBitmap;

    // Add these constants at the top of InventoryManagementActivity class
    private static final int LOW_STOCK_THRESHOLD = 10;
    private static final int CRITICAL_STOCK_THRESHOLD = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_management);

        firestore = FirebaseFirestore.getInstance();
        inventoryList = new ArrayList<>();

        initializeViews();
        setupClickListeners();
        loadInventory();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewInventory);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InventoryAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddItem).setOnClickListener(v -> showAddItemDialog());
        findViewById(R.id.btnViewArchived).setOnClickListener(v -> showArchivedItemsDialog());
    }

    private void showAddItemDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Add New Inventory Item");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_inventory_item, null);
        builder.setView(dialogView);

        EditText etProductName = dialogView.findViewById(R.id.etProductName);
        EditText etQuantity = dialogView.findViewById(R.id.etQuantity);
        EditText etPrice = dialogView.findViewById(R.id.etPrice);
        ImageView ivProductImage = dialogView.findViewById(R.id.ivProductImage);
        MaterialButton btnSelectImage = dialogView.findViewById(R.id.btnSelectImage);

        dialogSelectedBitmap = null;

        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
            currentDialogImageView = ivProductImage;
        });

        builder.setPositiveButton("Add", (dialog, which) -> {
            String productName = etProductName.getText().toString().trim();
            String quantityStr = etQuantity.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();

            if (productName.isEmpty() || quantityStr.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int quantity = Integer.parseInt(quantityStr);
                double price = Double.parseDouble(priceStr);

                if (dialogSelectedBitmap != null) {
                    uploadImageAndAddItem(dialogSelectedBitmap, productName, quantity, price);
                } else {
                    addInventoryItem(productName, quantity, price, null);
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        currentAddDialog = builder.create();
        currentAddDialog.show();
    }

    private void uploadImageAndAddItem(Bitmap imageBitmap, String productName, int quantity, double price) {
        Log.d("INVENTORY", "Starting image upload for: " + productName);
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading image...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] imageData = baos.toByteArray();
        Log.d("INVENTORY", "Image size: " + imageData.length + " bytes");

        String filename = "inventory_" + System.currentTimeMillis() + ".jpg";
        Log.d("INVENTORY", "Filename: " + filename);

        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference imageRef = storageRef.child("inventory_images/" + filename);

        UploadTask uploadTask = imageRef.putBytes(imageData);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            Log.d("INVENTORY", "Image uploaded to storage");
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String imageUrl = uri.toString();
                Log.d("INVENTORY", "Got download URL: " + imageUrl);
                progressDialog.dismiss();
                addInventoryItem(productName, quantity, price, imageUrl);
            }).addOnFailureListener(e -> {
                progressDialog.dismiss();
                Log.e("INVENTORY", "Failed to get download URL: " + e.getMessage(), e);
                Toast.makeText(this, "Failed to get image URL", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Log.e("INVENTORY", "Failed to upload image: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }).addOnProgressListener(snapshot -> {
            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
            progressDialog.setMessage("Uploading: " + (int) progress + "%");
        });
    }

    private void addInventoryItem(String productName, int quantity, double price, String imageUrl) {
        Log.d("INVENTORY", "Adding item - Name: " + productName + ", ImageURL: " + imageUrl);

        Map<String, Object> item = new HashMap<>();
        item.put("productName", productName);
        item.put("quantity", quantity);
        item.put("price", price);
        item.put("isArchived", false);
        item.put("timestamp", System.currentTimeMillis());

        if (imageUrl != null) {
            item.put("imageUrl", imageUrl);
            Log.d("INVENTORY", "Image URL added to Firestore: " + imageUrl);
        } else {
            Log.d("INVENTORY", "No image URL provided");
        }

        firestore.collection("inventory")
                .add(item)
                .addOnSuccessListener(documentReference -> {
                    Log.d("INVENTORY", "Item added with ID: " + documentReference.getId());
                    Toast.makeText(this, "Item added successfully", Toast.LENGTH_SHORT).show();
                    loadInventory();
                })
                .addOnFailureListener(e -> {
                    Log.e("INVENTORY", "Error adding item: " + e.getMessage(), e);
                    Toast.makeText(this, "Error adding item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                selectedImageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                dialogSelectedBitmap = selectedImageBitmap;

                if (currentDialogImageView != null && selectedImageBitmap != null) {
                    currentDialogImageView.setImageBitmap(selectedImageBitmap);
                }

                Log.d("INVENTORY", "Image selected and loaded successfully");

            } catch (IOException e) {
                Log.e("INVENTORY", "Error loading image: " + e.getMessage(), e);
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadInventory() {
        Log.d("INVENTORY", "Loading inventory...");
        firestore.collection("inventory")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    inventoryList.clear();
                    Log.d("INVENTORY", "Found " + queryDocumentSnapshots.size() + " documents");

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String id = document.getId();
                        String productName = document.getString("productName");
                        Long quantity = document.getLong("quantity");
                        Double price = document.getDouble("price");
                        Boolean isArchived = document.getBoolean("isArchived");
                        String imageUrl = document.getString("imageUrl");

                        Log.d("INVENTORY", "Document: " + productName + ", ImageURL: " + imageUrl);

                        boolean shouldAdd = (isArchived == null || !isArchived) &&
                                productName != null &&
                                quantity != null &&
                                price != null;

                        if (shouldAdd) {
                            InventoryItem item = new InventoryItem(id, productName,
                                    quantity.intValue(), price, imageUrl);
                            inventoryList.add(item);
                            Log.d("INVENTORY", "Added to list: " + productName);
                        }
                    }

                    inventoryList.sort((item1, item2) ->
                            item1.productName.compareToIgnoreCase(item2.productName));

                    adapter.notifyDataSetChanged();
                    Log.d("INVENTORY", "Adapter notified, items: " + inventoryList.size());

                    if (inventoryList.isEmpty()) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        tvEmptyState.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("INVENTORY", "Error loading inventory: " + e.getMessage(), e);
                    Toast.makeText(this, "Error loading inventory: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void archiveInventoryItem(String itemId, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Archive Item")
                .setMessage("Are you sure you want to archive this item?")
                .setPositiveButton("Archive", (dialog, which) -> {
                    firestore.collection("inventory").document(itemId)
                            .update("isArchived", true)
                            .addOnSuccessListener(aVoid -> {
                                inventoryList.remove(position);
                                adapter.notifyItemRemoved(position);
                                Toast.makeText(this, "Item archived", Toast.LENGTH_SHORT).show();

                                if (inventoryList.isEmpty()) {
                                    tvEmptyState.setVisibility(View.VISIBLE);
                                    recyclerView.setVisibility(View.GONE);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error archiving item", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showArchivedItemsDialog() {
        firestore.collection("inventory")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<InventoryItem> archivedItems = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String id = document.getId();
                        String productName = document.getString("productName");
                        Long quantity = document.getLong("quantity");
                        Double price = document.getDouble("price");
                        Boolean isArchived = document.getBoolean("isArchived");

                        if (isArchived != null && isArchived &&
                                productName != null && quantity != null && price != null) {
                            String imageUrl = document.getString("imageUrl");
                            InventoryItem item = new InventoryItem(id, productName, quantity.intValue(), price, imageUrl);
                            archivedItems.add(item);
                        }
                    }

                    archivedItems.sort((item1, item2) ->
                            item1.productName.compareToIgnoreCase(item2.productName));

                    if (archivedItems.isEmpty()) {
                        Toast.makeText(this, "No archived items found", Toast.LENGTH_SHORT).show();
                    } else {
                        showArchivedDialog(archivedItems);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading archived items", Toast.LENGTH_SHORT).show();
                });
    }

    private void showArchivedDialog(List<InventoryItem> archivedItems) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Archived Items (" + archivedItems.size() + ")");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_archived_items, null);
        builder.setView(dialogView);

        RecyclerView archivedRecyclerView = dialogView.findViewById(R.id.recyclerViewArchived);
        archivedRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        ArchivedAdapter archivedAdapter = new ArchivedAdapter(archivedItems);
        archivedRecyclerView.setAdapter(archivedAdapter);

        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private void unarchiveItem(String itemId) {
        firestore.collection("inventory").document(itemId)
                .update("isArchived", false)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Item unarchived successfully", Toast.LENGTH_SHORT).show();
                    loadInventory();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error unarchiving item", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateInventoryItem(String itemId, String productName, int quantity, double price, String imageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("productName", productName);
        updates.put("quantity", quantity);
        updates.put("price", price);

        if (imageUrl != null) {
            updates.put("imageUrl", imageUrl);
        }

        firestore.collection("inventory").document(itemId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Item updated successfully", Toast.LENGTH_SHORT).show();
                    loadInventory();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadImageAndUpdateItem(Bitmap imageBitmap, String itemId, String productName, int quantity, double price) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading image...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] imageData = baos.toByteArray();

        String filename = "inventory_" + System.currentTimeMillis() + ".jpg";
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference imageRef = storageRef.child("inventory_images/" + filename);

        UploadTask uploadTask = imageRef.putBytes(imageData);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                progressDialog.dismiss();
                String imageUrl = uri.toString();
                updateInventoryItem(itemId, productName, quantity, price, imageUrl);
            });
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
        });
    }

    private static class InventoryItem {
        String id;
        String productName;
        int quantity;
        double price;
        String imageUrl;

        InventoryItem(String id, String productName, int quantity, double price, String imageUrl) {
            this.id = id;
            this.productName = productName;
            this.quantity = quantity;
            this.price = price;
            this.imageUrl = imageUrl;
        }
    }

    private class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_inventory, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            InventoryItem item = inventoryList.get(position);

            Log.d("INVENTORY", "Binding item: " + item.productName + ", Image: " + item.imageUrl);

            holder.tvProductName.setText(item.productName);
            holder.tvQuantity.setText(String.valueOf(item.quantity));
            holder.tvPrice.setText(String.format("₱%.2f", item.price));

            // Add stock level indication
            // Replace the color setting section with:
            if (item.quantity <= CRITICAL_STOCK_THRESHOLD) {
                // Critical stock - RED
                holder.tvQuantity.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.stock_critical));
                holder.tvQuantity.setTypeface(null, Typeface.BOLD);
            } else if (item.quantity <= LOW_STOCK_THRESHOLD) {
                // Low stock - YELLOW/ORANGE
                holder.tvQuantity.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.stock_low));
                holder.tvQuantity.setTypeface(null, Typeface.BOLD);
            } else {
                // Normal stock
                holder.tvQuantity.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.stock_normal));
                holder.tvQuantity.setTypeface(null, Typeface.NORMAL);
            }

            if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                Log.d("INVENTORY", "Loading image with Glide: " + item.imageUrl);
                Glide.with(holder.itemView.getContext())
                        .load(item.imageUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image)
                        .into(holder.ivProductImage);
            } else {
                Log.d("INVENTORY", "No image URL, showing placeholder");
                holder.ivProductImage.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            holder.btnEdit.setOnClickListener(v -> showEditDialog(item, position));
            holder.btnArchive.setOnClickListener(v -> archiveInventoryItem(item.id, position));
        }

        @Override
        public int getItemCount() {
            return inventoryList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvProductName, tvQuantity, tvPrice;
            ImageButton btnEdit, btnArchive;
            ImageView ivProductImage;

            ViewHolder(View itemView) {
                super(itemView);
                tvProductName = itemView.findViewById(R.id.tvProductName);
                tvQuantity = itemView.findViewById(R.id.tvQuantity);
                tvPrice = itemView.findViewById(R.id.tvPrice);
                btnEdit = itemView.findViewById(R.id.btnEdit);
                btnArchive = itemView.findViewById(R.id.btnArchive);
                ivProductImage = itemView.findViewById(R.id.ivProductImage);
            }
        }
    }

    private class ArchivedAdapter extends RecyclerView.Adapter<ArchivedAdapter.ViewHolder> {
        private List<InventoryItem> archivedItems;

        ArchivedAdapter(List<InventoryItem> archivedItems) {
            this.archivedItems = archivedItems;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_archived_inventory, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            InventoryItem item = archivedItems.get(position);

            holder.tvProductName.setText(item.productName);
            holder.tvQuantity.setText(String.valueOf(item.quantity));
            holder.tvPrice.setText(String.format("₱%.2f", item.price));

            // Add the same color coding for archived items
            if (item.quantity <= CRITICAL_STOCK_THRESHOLD) {
                holder.tvQuantity.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_red_dark));
            } else if (item.quantity <= LOW_STOCK_THRESHOLD) {
                holder.tvQuantity.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_orange_dark));
            } else {
                holder.tvQuantity.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.black));
            }

            holder.btnUnarchive.setOnClickListener(v -> {
                unarchiveItem(item.id);
                archivedItems.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, archivedItems.size());
            });
        }

        @Override
        public int getItemCount() {
            return archivedItems.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvProductName, tvQuantity, tvPrice;
            MaterialButton btnUnarchive;

            ViewHolder(View itemView) {
                super(itemView);
                tvProductName = itemView.findViewById(R.id.tvProductName);
                tvQuantity = itemView.findViewById(R.id.tvQuantity);
                tvPrice = itemView.findViewById(R.id.tvPrice);
                btnUnarchive = itemView.findViewById(R.id.btnUnarchive);
            }
        }
    }

    private void showEditDialog(InventoryItem item, int position) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Edit Inventory Item");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_inventory_item, null);
        builder.setView(dialogView);

        EditText etProductName = dialogView.findViewById(R.id.etProductName);
        EditText etQuantity = dialogView.findViewById(R.id.etQuantity);
        EditText etPrice = dialogView.findViewById(R.id.etPrice);
        ImageView ivProductImage = dialogView.findViewById(R.id.ivProductImage);
        MaterialButton btnSelectImage = dialogView.findViewById(R.id.btnSelectImage);

        etProductName.setText(item.productName);
        etQuantity.setText(String.valueOf(item.quantity));
        etPrice.setText(String.valueOf(item.price));

        if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(item.imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(ivProductImage);
        }

        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
            currentDialogImageView = ivProductImage;
        });

        builder.setPositiveButton("Update", (dialog, which) -> {
            String productName = etProductName.getText().toString().trim();
            String quantityStr = etQuantity.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();

            if (productName.isEmpty() || quantityStr.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int quantity = Integer.parseInt(quantityStr);
                double price = Double.parseDouble(priceStr);

                if (dialogSelectedBitmap != null) {
                    uploadImageAndUpdateItem(dialogSelectedBitmap, item.id, productName, quantity, price);
                } else {
                    updateInventoryItem(item.id, productName, quantity, price, item.imageUrl);
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}