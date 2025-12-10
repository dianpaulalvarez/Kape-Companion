package com.lu.coffeecompanion.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.lu.coffeecompanion.R;
import com.lu.coffeecompanion.models.User;

import java.util.ArrayList;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private Context context;
    private ArrayList<User> userList;
    private DatabaseReference dbRef;

    public UserAdapter(Context context, ArrayList<User> userList) {
        this.context = context;
        this.userList = userList;
        this.dbRef = FirebaseDatabase.getInstance().getReference("users");
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        Log.d("UserAdapter", "Binding user: " + user.getName() + ", Blocked: " + user.isBlocked() + ", Role: " + user.getRole());

        holder.tvName.setText(user.getName());
        holder.tvEmail.setText(user.getEmail());
        holder.tvMobile.setText("Mobile: " + user.getMobile());
        holder.tvRole.setText("Role: " + user.getRole());

        // Update status display
        if (user.isBlocked()) {
            holder.tvStatus.setText("BLOCKED");
            holder.tvStatus.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
            holder.btnBlock.setText("UNBLOCK");
            holder.btnBlock.setBackgroundTintList(ContextCompat.getColorStateList(context, android.R.color.holo_green_dark));
            holder.tvName.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
        } else {
            holder.tvStatus.setText("ACTIVE");
            holder.tvStatus.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
            holder.btnBlock.setText("BLOCK");
            holder.btnBlock.setBackgroundTintList(ContextCompat.getColorStateList(context, android.R.color.holo_orange_dark));
            holder.tvName.setTextColor(ContextCompat.getColor(context, android.R.color.black));
        }

        // Button click listeners
        holder.btnEdit.setOnClickListener(v -> showEditDialog(user, position));
        holder.btnBlock.setOnClickListener(v -> toggleBlockUser(user, position));
        holder.btnDelete.setOnClickListener(v -> deleteUser(user, position));
    }

    private void showEditDialog(User user, int position) {
        // Create custom dialog for editing user
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit User");

        // Inflate the edit dialog layout
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_user, null);
        builder.setView(dialogView);

        // Initialize dialog fields
        TextInputEditText etName = dialogView.findViewById(R.id.etName);
        TextInputEditText etEmail = dialogView.findViewById(R.id.etEmail);
        TextInputEditText etMobile = dialogView.findViewById(R.id.etMobile);
        TextInputEditText etRole = dialogView.findViewById(R.id.etRole);

        // Pre-fill current user data
        etName.setText(user.getName());
        etEmail.setText(user.getEmail());
        etMobile.setText(user.getMobile());
        etRole.setText(user.getRole());

        builder.setPositiveButton("Save", (dialog, which) -> {
            // Get updated values
            String newName = etName.getText().toString().trim();
            String newEmail = etEmail.getText().toString().trim();
            String newMobile = etMobile.getText().toString().trim();
            String newRole = etRole.getText().toString().trim();

            // Validate inputs
            if (newName.isEmpty() || newEmail.isEmpty()) {
                Toast.makeText(context, "Name and Email are required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update user in Firebase
            updateUserInFirebase(user, newName, newEmail, newMobile, newRole, position);
        });

        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateUserInFirebase(User user, String newName, String newEmail, String newMobile, String newRole, int position) {
        DatabaseReference userRef = dbRef.child(user.getId());

        userRef.child("name").setValue(newName);
        userRef.child("email").setValue(newEmail);
        userRef.child("mobile").setValue(newMobile);
        userRef.child("role").setValue(newRole)
                .addOnSuccessListener(aVoid -> {
                    // Update local user object
                    user.setName(newName);
                    user.setEmail(newEmail);
                    user.setMobile(newMobile);
                    user.setRole(newRole);

                    // Notify adapter
                    notifyItemChanged(position);

                    Toast.makeText(context, "User updated successfully!", Toast.LENGTH_SHORT).show();
                    Log.d("UserAdapter", "User updated: " + newName);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to update user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("UserAdapter", "Update error: " + e.getMessage());
                });
    }

    private void toggleBlockUser(User user, int position) {
        boolean newBlockedStatus = !user.isBlocked();
        String action = newBlockedStatus ? "block" : "unblock";

        new AlertDialog.Builder(context)
                .setTitle((newBlockedStatus ? "Block" : "Unblock") + " User")
                .setMessage("Are you sure you want to " + action + " " + user.getName() + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    dbRef.child(user.getId()).child("blocked").setValue(newBlockedStatus)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context, "User " + action + "ed successfully!", Toast.LENGTH_SHORT).show();
                                user.setBlocked(newBlockedStatus);
                                notifyItemChanged(position);
                                Log.d("UserAdapter", "User " + action + "ed: " + user.getName());
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to " + action + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e("UserAdapter", action + " error: " + e.getMessage());
                            });
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteUser(User user, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete " + user.getName() + "? This action cannot be undone.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    dbRef.child(user.getId()).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context, "User deleted successfully!", Toast.LENGTH_SHORT).show();
                                userList.remove(position);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, userList.size());
                                Log.d("UserAdapter", "User deleted: " + user.getName());
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e("UserAdapter", "Delete error: " + e.getMessage());
                            });
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvMobile, tvRole, tvStatus;
        Button btnEdit, btnBlock, btnDelete;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvMobile = itemView.findViewById(R.id.tvMobile);
            tvRole = itemView.findViewById(R.id.tvRole);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnBlock = itemView.findViewById(R.id.btnBlock);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}