package com.lu.coffeecompanion;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lu.coffeecompanion.databinding.ItemCartBinding;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private Context context;
    private List<CartItem> cartItems;
    private boolean isEditMode = false;
    private String userId;
    private FirebaseFirestore db;

    // Callback interface to notify activity about total price changes
    public interface OnQuantityChangeListener {
        void onQuantityChanged();
    }

    private OnQuantityChangeListener quantityChangeListener;

    public CartAdapter(Context context, List<CartItem> cartItems, String userId, OnQuantityChangeListener listener) {
        this.context = context;
        this.cartItems = cartItems;
        this.userId = userId;
        this.quantityChangeListener = listener;
        this.db = FirebaseFirestore.getInstance();
    }

    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCartBinding binding = ItemCartBinding.inflate(LayoutInflater.from(context), parent, false);
        return new CartViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);

        // SET ITEM DATA
        holder.binding.itemName.setText(item.getName());
        holder.binding.itemPrice.setText("₱" + String.format("%.2f", item.getPrice()));
        holder.binding.itemShop.setText(item.getShop());
        holder.binding.quantity.setText(String.valueOf(item.getQuantity()));

        Glide.with(context).load(item.getImageUrl()).into(holder.binding.itemImage);

        // CHECKBOX FOR EDIT MODE
        holder.binding.cartCheckbox.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        holder.binding.cartCheckbox.setChecked(item.isSelected());
        holder.binding.cartCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> item.setSelected(isChecked));

        // ITEM CLICK (OPEN EDIT PAGE WHEN NOT IN EDIT MODE)
        if (!isEditMode) {
            holder.binding.linearItemCart.setOnClickListener(v -> {
                Intent intent = new Intent(context, EditItemCartActivity.class);
                intent.putExtra("cartItemId", item.getCartId());
                context.startActivity(intent);
            });
        } else {
            holder.binding.linearItemCart.setOnClickListener(null);
        }

        // PLUS BUTTON
        holder.binding.btnAdd.setOnClickListener(v -> {
            int qty = item.getQuantity();
            item.setQuantity(qty + 1);
            holder.binding.quantity.setText(String.valueOf(item.getQuantity()));

            // UPDATE FIRESTORE
            db.collection("users").document(userId)
                    .collection("cart").document(item.getCartId())
                    .update("quantity", item.getQuantity());

            // NOTIFY TOTAL CHANGE
            if (quantityChangeListener != null) quantityChangeListener.onQuantityChanged();
        });

        // MINUS BUTTON
        holder.binding.btnMinus.setOnClickListener(v -> {
            int qty = item.getQuantity();
            if (qty > 1) {
                item.setQuantity(qty - 1);
                holder.binding.quantity.setText(String.valueOf(item.getQuantity()));

                db.collection("users").document(userId)
                        .collection("cart").document(item.getCartId())
                        .update("quantity", item.getQuantity());

                if (quantityChangeListener != null) quantityChangeListener.onQuantityChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        ItemCartBinding binding;

        public CartViewHolder(@NonNull ItemCartBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
