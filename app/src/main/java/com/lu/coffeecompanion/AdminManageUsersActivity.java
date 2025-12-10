package com.lu.coffeecompanion;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.lu.coffeecompanion.adapters.UserAdapter;
import com.lu.coffeecompanion.models.User;

import java.util.ArrayList;

public class AdminManageUsersActivity extends AppCompatActivity {

    private RecyclerView recyclerUsers;
    private UserAdapter adapter;
    private ArrayList<User> userList;
    private DatabaseReference dbRef;
    private ProgressBar progressBar;
    private ImageView btnBack;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_users);

        // Initialize views
        recyclerUsers = findViewById(R.id.recyclerUsers);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        // Check if user is logged in
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Check if user is admin
        if (!currentUser.getEmail().equals("admin@gmail.com")) {
            Toast.makeText(this, "Access denied. Admin only.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup RecyclerView
        recyclerUsers.setLayoutManager(new LinearLayoutManager(this));
        userList = new ArrayList<>();
        adapter = new UserAdapter(this, userList);
        recyclerUsers.setAdapter(adapter);

        // Initialize Firebase
        dbRef = FirebaseDatabase.getInstance().getReference("users");

        // Setup back button
        btnBack.setOnClickListener(v -> finish());

        // Load users from Firebase
        loadUsers();
    }

    private void loadUsers() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerUsers.setVisibility(View.GONE);

        Log.d("AdminManageUsers", "Starting to load users...");
        Log.d("AdminManageUsers", "Current user: " + (currentUser != null ? currentUser.getEmail() : "null"));

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("AdminManageUsers", "DataSnapshot exists: " + snapshot.exists());
                Log.d("AdminManageUsers", "Children count: " + snapshot.getChildrenCount());

                userList.clear();

                if (snapshot.exists()) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        try {
                            String userId = userSnapshot.getKey();
                            Log.d("AdminManageUsers", "Processing user: " + userId);

                            // Get individual fields
                            String name = getStringValue(userSnapshot, "name");
                            String email = getStringValue(userSnapshot, "email");
                            String mobile = getStringValue(userSnapshot, "mobile");
                            String role = getStringValue(userSnapshot, "role");
                            Boolean blocked = getBooleanValue(userSnapshot, "blocked");

                            Log.d("AdminManageUsers", "User data - Name: " + name + ", Email: " + email + ", Role: " + role + ", Blocked: " + blocked);

                            // Create user object
                            User user = new User();
                            user.setId(userId);
                            user.setName(name != null ? name : "Unknown User");
                            user.setEmail(email != null ? email : "No email");
                            user.setMobile(mobile != null ? mobile : "Not set");
                            user.setRole(role != null ? role : "user");
                            user.setBlocked(blocked != null ? blocked : false);

                            userList.add(user);

                            Log.d("AdminManageUsers", "User added to list: " + user.getName());

                        } catch (Exception e) {
                            Log.e("AdminManageUsers", "Error loading user: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }

                    Log.d("AdminManageUsers", "Total users loaded: " + userList.size());
                    adapter.notifyDataSetChanged();

                    if (userList.isEmpty()) {
                        Toast.makeText(AdminManageUsersActivity.this, "No users found in database", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(AdminManageUsersActivity.this, "Loaded " + userList.size() + " users", Toast.LENGTH_SHORT).show();
                    }

                    recyclerUsers.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(AdminManageUsersActivity.this, "No users data found", Toast.LENGTH_SHORT).show();
                }

                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AdminManageUsers", "Database error: " + error.getMessage());
                Toast.makeText(AdminManageUsersActivity.this,
                        "Failed to load users: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
                progressBar.setVisibility(View.GONE);
                recyclerUsers.setVisibility(View.VISIBLE);
            }
        });
    }

    private String getStringValue(DataSnapshot snapshot, String key) {
        try {
            return snapshot.child(key).getValue(String.class);
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean getBooleanValue(DataSnapshot snapshot, String key) {
        try {
            return snapshot.child(key).getValue(Boolean.class);
        } catch (Exception e) {
            return false;
        }
    }
}