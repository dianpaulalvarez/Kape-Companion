package com.lu.coffeecompanion;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
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
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.lu.coffeecompanion.databinding.ActivityRegisterBinding;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    ActivityRegisterBinding binding;
    FirebaseAuth mAuth;
    DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("users");

        binding.btnLogin.setPaintFlags(binding.btnLogin.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        // Go to LoginActivity
        binding.btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();
        });

        // Register button
        binding.btnRegister.setOnClickListener(v -> {
            binding.progressBar.setVisibility(View.VISIBLE);

            String email = binding.editEmail.getText().toString().trim();
            String password = binding.editPassword.getText().toString().trim();
            String confirmPassword = binding.editConfirmPassword.getText().toString().trim();
            String name = binding.editName.getText().toString().trim(); // <== make sure may Name field sa XML mo

            // Validation
            if (name.isEmpty()) {
                binding.editName.setError("Enter your name");
                binding.progressBar.setVisibility(View.GONE);
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.editEmail.setError("Enter a valid email");
                binding.progressBar.setVisibility(View.GONE);
                return;
            }
            if (password.length() < 6) {
                binding.editPassword.setError("Minimum 6 characters");
                binding.progressBar.setVisibility(View.GONE);
                return;
            }
            if (!password.equals(confirmPassword)) {
                binding.editConfirmPassword.setError("Passwords do not match");
                binding.progressBar.setVisibility(View.GONE);
                return;
            }

            // Create new user
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        binding.progressBar.setVisibility(View.GONE);

                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Send verification email
                                user.sendEmailVerification()
                                        .addOnCompleteListener(emailTask -> {
                                            if (emailTask.isSuccessful()) {
                                                // Save user data to Realtime Database
                                                String userId = user.getUid();

                                                Map<String, Object> userData = new HashMap<>();
                                                userData.put("id", userId);
                                                userData.put("name", name);
                                                userData.put("email", email);
                                                userData.put("role", "user");
                                                userData.put("verified", false);

                                                databaseRef.child(userId).setValue(userData)
                                                        .addOnSuccessListener(aVoid -> {
                                                            Toast.makeText(RegisterActivity.this,
                                                                    "Registration successful! Please verify your email.",
                                                                    Toast.LENGTH_LONG).show();
                                                        })
                                                        .addOnFailureListener(e ->
                                                                Toast.makeText(RegisterActivity.this,
                                                                        "Failed to save user data.", Toast.LENGTH_SHORT).show());

                                                // Sign out and redirect to login
                                                mAuth.signOut();
                                                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                Toast.makeText(RegisterActivity.this,
                                                        "Failed to send verification email. Try again later.",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        } else {
                            Exception e = task.getException();
                            if (e instanceof FirebaseAuthException) {
                                String code = ((FirebaseAuthException) e).getErrorCode();
                                if (code.equals("ERROR_EMAIL_ALREADY_IN_USE")) {
                                    binding.editEmail.setError("Email already in use. Try logging in.");
                                } else {
                                    Toast.makeText(RegisterActivity.this,
                                            "Registration failed: " + code, Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(RegisterActivity.this,
                                        "Something went wrong. Please try again.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });
    }
}
