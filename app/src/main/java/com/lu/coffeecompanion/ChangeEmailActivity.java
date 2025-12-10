package com.lu.coffeecompanion;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lu.coffeecompanion.databinding.ActivityChangeEmailBinding;

public class ChangeEmailActivity extends AppCompatActivity {

    ActivityChangeEmailBinding binding;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseAuth auth = FirebaseAuth.getInstance();
    FirebaseUser currentUser = auth.getCurrentUser();
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        if (currentUser != null) {
            userId = currentUser.getUid();
        }
        else {
            Intent intent = new Intent (getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();
        }

        binding = ActivityChangeEmailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());



        binding.btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.progressBar.setVisibility(View.VISIBLE);
                String newEmail = binding.inputEmail.getText().toString().trim();
                String password = binding.inputPassword.getText().toString().trim();
                AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), password);

                if(newEmail.isEmpty()){
                    binding.inputEmail.setError("Email cannot be empty.");
                    binding.inputEmail.requestFocus();
                    binding.progressBar.setVisibility(View.GONE);
                } else if (password.isEmpty()) {
                    binding.inputPassword.setError("Password cannot be empty.");
                    binding.inputPassword.requestFocus();
                    binding.progressBar.setVisibility(View.GONE);
                } else if (password.length() < 6) {
                    binding.inputPassword.setError("Password must be at least 6 characters");
                    binding.inputPassword.requestFocus();
                    binding.progressBar.setVisibility(View.GONE);
                } else if (!isValidEmail(newEmail)) {
                    binding.inputEmail.setError("Invalid email format.");
                    binding.inputEmail.requestFocus();
                    binding.progressBar.setVisibility(View.GONE);
                }
                else{
                    currentUser.reauthenticate(credential).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        currentUser.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener(emailtask -> {
                            if (emailtask.isSuccessful()) {
                                currentUser.reload();
                                Toast.makeText(getApplicationContext(), "Verification email sent to " + newEmail, Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                binding.progressBar.setVisibility(View.GONE);
                                Toast.makeText(getApplicationContext(), "Error updating email: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(getApplicationContext(), "Reauthentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

                }



            }
        });



    }
    private boolean isValidEmail(String email) {
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        return email.matches(emailPattern);
    }
}