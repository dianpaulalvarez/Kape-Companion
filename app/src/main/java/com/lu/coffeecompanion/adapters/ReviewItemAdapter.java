package com.lu.coffeecompanion.adapters;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lu.coffeecompanion.R;

import java.util.List;

public class ReviewItemAdapter extends RecyclerView.Adapter<ReviewItemAdapter.ViewHolder> {

    private List<ReviewItem> reviewItems;
    private ReviewItemListener listener;

    public interface ReviewItemListener {
        void onRatingChanged(int position, float rating);
        void onCommentChanged(int position, String comment);
    }

    public ReviewItemAdapter(List<ReviewItem> reviewItems, ReviewItemListener listener) {
        this.reviewItems = reviewItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review_input, parent, false);
        return new ViewHolder(view);
    }
    // In ReviewItemAdapter.java, update the layout binding:
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        ReviewItem item = reviewItems.get(position);

        holder.tvItemName.setText(item.getItemName());
        holder.ratingBar.setRating(item.getRating());
        holder.etComment.setText(item.getComment());

        // Shopee-style: Show rating value
        holder.tvRatingValue.setText(String.format("%.1f", item.getRating()));

        // Character counter
        int charCount = item.getComment() != null ? item.getComment().length() : 0;
        holder.tvCharCount.setText(charCount + "/500");

        // Update color based on length (Shopee-style)
        if (charCount > 450) {
            holder.tvCharCount.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
        } else if (charCount > 400) {
            holder.tvCharCount.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_orange_dark));
        } else {
            holder.tvCharCount.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.darker_gray));
        }

        // Rating change listener
        holder.ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (fromUser && listener != null) {
                listener.onRatingChanged(position, rating);
                holder.tvRatingValue.setText(String.format("%.1f", rating));
            }
        });

        // Comment change listener
        holder.etComment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String comment = s.toString();
                if (listener != null) {
                    listener.onCommentChanged(position, comment);
                }

                // Update character count
                int length = comment.length();
                holder.tvCharCount.setText(length + "/500");

                // Limit to 500 characters
                if (length > 500) {
                    holder.etComment.setText(comment.substring(0, 500));
                    holder.etComment.setSelection(500);
                }
            }
        });
    }

    private void updateCharCountColor(TextView tvCharCount, int length) {
        if (length > 450) {
            tvCharCount.setTextColor(tvCharCount.getContext().getResources().getColor(R.color.red));
        } else if (length > 400) {
            tvCharCount.setTextColor(tvCharCount.getContext().getResources().getColor(R.color.orange));
        } else {
            tvCharCount.setTextColor(tvCharCount.getContext().getResources().getColor(R.color.muted));
        }
    }

    @Override
    public int getItemCount() {
        return reviewItems.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName;
        RatingBar ratingBar;
        TextView tvRatingValue;
        EditText etComment;
        TextView tvCharCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            tvRatingValue = itemView.findViewById(R.id.tvRatingValue);
            etComment = itemView.findViewById(R.id.etComment);
            tvCharCount = itemView.findViewById(R.id.tvCharCount);
        }
    }

    // Add this inner class
    public static class ReviewItem {
        private String itemId;
        private String shopId;
        private String itemName;
        private float rating;
        private String comment;

        public String getItemId() { return itemId; }
        public void setItemId(String itemId) { this.itemId = itemId; }
        public String getShopId() { return shopId; }
        public void setShopId(String shopId) { this.shopId = shopId; }
        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }
        public float getRating() { return rating; }
        public void setRating(float rating) { this.rating = rating; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }
}