package com.lu.coffeecompanion;

import android.app.Dialog;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

public class RatingDialog {

    public static void showRatingDialog(Context context, String orderId, String menuItemId,
                                        String itemName, RatingCallback callback) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_rating);
        dialog.getWindow().setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );

        RatingBar ratingBar = dialog.findViewById(R.id.rating_bar);
        EditText feedbackText = dialog.findViewById(R.id.feedback_text);
        Button submitBtn = dialog.findViewById(R.id.submit_btn);
        Button cancelBtn = dialog.findViewById(R.id.cancel_btn);

        submitBtn.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            String feedback = feedbackText.getText().toString().trim();

            if (rating == 0) {
                Toast.makeText(context, "Please select a rating", Toast.LENGTH_SHORT).show();
                return;
            }

            if (feedback.isEmpty()) {
                Toast.makeText(context, "Please write feedback", Toast.LENGTH_SHORT).show();
                return;
            }

            callback.onRatingSubmitted(orderId, menuItemId, itemName, rating, feedback);
            dialog.dismiss();
        });

        cancelBtn.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    public interface RatingCallback {
        void onRatingSubmitted(String orderId, String menuItemId, String itemName, float rating, String feedback);
    }
}
