package com.lu.coffeecompanion;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.lu.coffeecompanion.databinding.FragmentAdminAccountBinding;

import java.util.HashMap;
import java.util.Map;

public class AdminAccountFragment extends Fragment {

    private FragmentAdminAccountBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference db;

    public AdminAccountFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAdminAccountBinding.inflate(inflater, container, false);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseDatabase.getInstance().getReference();

        if (currentUser != null) {
            loadAdminData();
        }

        // Logout button - DAPAT MAG-SIGNOUT AT PUMUNTA SA LOGIN
        binding.btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        mAuth.signOut();
                        Intent intent = new Intent(requireContext(), LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        return binding.getRoot();
    }

    private void loadAdminData() {
        String userId = currentUser.getUid();

        db.child("users").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String name = dataSnapshot.child("name").getValue(String.class);
                            String email = dataSnapshot.child("email").getValue(String.class);
                            String mobile = dataSnapshot.child("mobile").getValue(String.class);
                            String role = dataSnapshot.child("role").getValue(String.class);
                            Boolean blocked = dataSnapshot.child("blocked").getValue(Boolean.class);

                            binding.textName.setText(name != null ? name : "Admin Account");
                            binding.email.setText(email != null ? email : currentUser.getEmail());
                            binding.mobile.setText(mobile != null ? mobile : "Not Available");
                        } else {
                            // Create admin data if doesn't exist
                            createAdminData();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        binding.textName.setText("Admin Account");
                        binding.email.setText(currentUser.getEmail());
                        binding.mobile.setText("Not Available");
                    }
                });
    }

    private void createAdminData() {
        String userId = currentUser.getUid();
        Map<String, Object> adminData = new HashMap<>();
        adminData.put("name", "Admin Account");
        adminData.put("email", currentUser.getEmail());
        adminData.put("mobile", "Not Available");
        adminData.put("role", "admin");
        adminData.put("blocked", false);

        db.child("users").child(userId)
                .setValue(adminData)
                .addOnSuccessListener(aVoid -> {
                    loadAdminData();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}