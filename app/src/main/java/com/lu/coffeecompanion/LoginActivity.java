package com.lu.coffeecompanion;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.lu.coffeecompanion.databinding.ActivityLoginBinding;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    ActivityLoginBinding binding;
    FirebaseAuth mAuth;
    DatabaseReference dbRef;

    // Admin credentials
    private static final String ADMIN_EMAIL = "admin@gmail.com";
    private static final String ADMIN_PASSWORD = "admin123";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();

        // Underline register text
        binding.btnRegister.setPaintFlags(binding.btnRegister.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        // Go to RegisterActivity
        binding.btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
            startActivity(intent);
        });

        // LOGIN BUTTON CLICK
        binding.btnLogin.setOnClickListener(v -> {
            binding.progressBar.setVisibility(View.VISIBLE);

            String email = binding.editEmail.getText().toString().trim();
            String password = binding.editPassword.getText().toString().trim();

            // Basic validations
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.editEmail.setError("Please enter a valid email.");
                binding.editEmail.requestFocus();
                binding.progressBar.setVisibility(View.GONE);
                return;
            }

            if (password.isEmpty()) {
                binding.editPassword.setError("Password cannot be empty.");
                binding.editPassword.requestFocus();
                binding.progressBar.setVisibility(View.GONE);
                return;
            }

            // Check if it's admin login
            if (email.equals(ADMIN_EMAIL) && password.equals(ADMIN_PASSWORD)) {
                loginAdmin();
            } else {
                // Regular user login
                loginRegularUser(email, password);
            }
        });
    }

    private void loginAdmin() {
        mAuth.signInWithEmailAndPassword(ADMIN_EMAIL, ADMIN_PASSWORD)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                handleAdminLoginSuccess(user);
                            }
                        } else {
                            // If admin doesn't exist in Firebase Auth, create it
                            createAdminAccount();
                        }
                    }
                });
    }

    private void handleAdminLoginSuccess(FirebaseUser user) {
        // Check if admin data exists in database
        dbRef.child("users").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                binding.progressBar.setVisibility(View.GONE);

                if (!snapshot.exists()) {
                    // Create admin data in database
                    createAdminData(user.getUid());
                }

                Toast.makeText(LoginActivity.this, "Welcome Admin!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(LoginActivity.this, "Welcome Admin!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void createAdminAccount() {
        mAuth.createUserWithEmailAndPassword(ADMIN_EMAIL, ADMIN_PASSWORD)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Create admin data in database
                                createAdminData(user.getUid());
                                Toast.makeText(LoginActivity.this, "Admin account created! Welcome Admin!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }
                        } else {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(LoginActivity.this,
                                    "Failed to login: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void createAdminData(String userId) {
        Map<String, Object> adminData = new HashMap<>();
        adminData.put("name", "Administrator");
        adminData.put("email", ADMIN_EMAIL);
        adminData.put("mobile", "Not Available");
        adminData.put("role", "admin");
        adminData.put("blocked", false);

        dbRef.child("users").child(userId).setValue(adminData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("LoginActivity", "Admin data created successfully");
                    } else {
                        Log.e("LoginActivity", "Failed to create admin data: " + task.getException().getMessage());
                    }
                });
    }

    private void loginRegularUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        binding.progressBar.setVisibility(View.GONE);

                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                if (user.isEmailVerified()) {
                                    handleRegularUserLoginSuccess(user);
                                } else {
                                    Toast.makeText(LoginActivity.this, "Please verify your email first.", Toast.LENGTH_LONG).show();
                                    mAuth.signOut(); // Sign out if email not verified
                                }
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Invalid email or password.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void handleRegularUserLoginSuccess(FirebaseUser user) {
        // Check if user data exists in database
        dbRef.child("users").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    // Create user data with default values
                    createUserData(user.getUid(), user.getEmail(), user.getDisplayName());
                    proceedToUserDashboard();
                } else {
                    // Check if account is blocked
                    Boolean isBlocked = snapshot.child("blocked").getValue(Boolean.class);
                    if (isBlocked != null && isBlocked) {
                        // Account is blocked
                        mAuth.signOut();
                        Toast.makeText(LoginActivity.this,
                                "Your account has been blocked. Please contact administrator.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        // Account is not blocked, proceed to dashboard
                        proceedToUserDashboard();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LoginActivity.this, "Error checking account status.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void proceedToUserDashboard() {
        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getApplicationContext(), UserDashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void createUserData(String userId, String email, String displayName) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", displayName != null ? displayName : "User");
        userData.put("email", email);
        userData.put("mobile", "Not set");
        userData.put("role", "user");
        userData.put("blocked", false);

        dbRef.child("users").child(userId).setValue(userData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("LoginActivity", "User data created successfully");
                    } else {
                        Log.e("LoginActivity", "Failed to create user data: " + task.getException().getMessage());
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already logged in when activity starts
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            redirectToAppropriateDashboard(currentUser);
        }
    }

    private void redirectToAppropriateDashboard(FirebaseUser user) {
        if (user.getEmail().equals(ADMIN_EMAIL)) {
            // Redirect to admin dashboard
            Intent intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            // Check if regular user is blocked
            dbRef.child("users").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Boolean isBlocked = snapshot.child("blocked").getValue(Boolean.class);
                        if (isBlocked != null && isBlocked) {
                            // Account is blocked, sign out
                            mAuth.signOut();
                            Toast.makeText(LoginActivity.this,
                                    "Your account has been blocked. Please contact administrator.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    // Redirect to user dashboard if not blocked
                    Intent intent = new Intent(LoginActivity.this, UserDashboardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // On error, just redirect (fail-safe)
                    Intent intent = new Intent(LoginActivity.this, UserDashboardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            });
        }
    }
}