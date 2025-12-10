package com.lu.coffeecompanion;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.github.chrisbanes.photoview.PhotoView;
import com.bumptech.glide.Glide;

public class PaymentProofViewerActivity extends AppCompatActivity {

    PhotoView photoView;
    Button btnDelete;
    String img;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_proof_viewer);

        photoView = findViewById(R.id.photoView);
        btnDelete = findViewById(R.id.btnDelete);
        img = getIntent().getStringExtra("img");

        // Use built-in placeholder to avoid missing resource errors.
        int placeholder = android.R.drawable.ic_menu_report_image;

        if (img == null || img.isEmpty()) {
            Glide.with(this)
                    .load(placeholder)
                    .into(photoView);
            // Disable delete button if no image
            btnDelete.setEnabled(false);
        } else {
            Glide.with(this)
                    .load(img)
                    .placeholder(placeholder)
                    .error(placeholder)
                    .into(photoView);
        }

        // Delete button click listener
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteConfirmationDialog();
            }
        });
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Payment Proof");
        builder.setMessage("Are you sure you want to delete this payment proof?");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            deletePaymentProof();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deletePaymentProof() {
        // Dito mo ilalagay ang logic para sa pag-delete
        // Halimbawa:
        // - Delete from local storage
        // - Delete from database
        // - Update server, etc.

        // For now, show a toast message and close the activity
        Toast.makeText(this, "Payment proof deleted", Toast.LENGTH_SHORT).show();

        // Set result if needed
        setResult(RESULT_OK);
        finish();
    }
}