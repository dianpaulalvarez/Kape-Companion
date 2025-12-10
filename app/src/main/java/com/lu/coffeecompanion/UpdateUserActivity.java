package com.lu.coffeecompanion;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class UpdateUserActivity extends AppCompatActivity {

    EditText etName, etEmail, etContact;
    Button btnUpdate;
    String userId;
    DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_user);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etContact = findViewById(R.id.etContact);
        btnUpdate = findViewById(R.id.btnUpdate);

        dbRef = FirebaseDatabase.getInstance().getReference("Users");

        userId = getIntent().getStringExtra("userId");
        etName.setText(getIntent().getStringExtra("name"));
        etEmail.setText(getIntent().getStringExtra("email"));
        etContact.setText(getIntent().getStringExtra("contact"));

        btnUpdate.setOnClickListener(v -> {
            String name = etName.getText().toString();
            String email = etEmail.getText().toString();
            String contact = etContact.getText().toString();

            if (name.isEmpty() || email.isEmpty() || contact.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            UserModel updatedUser = new UserModel(userId, name, email, contact);
            dbRef.child(userId).setValue(updatedUser);
            Toast.makeText(this, "User Updated!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
