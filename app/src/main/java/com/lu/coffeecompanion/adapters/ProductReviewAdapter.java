package com.lu.coffeecompanion.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lu.coffeecompanion.R;
import com.lu.coffeecompanion.models.ProductReview;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ProductReviewAdapter extends RecyclerView.Adapter<ProductReviewAdapter.ReviewViewHolder> {

    private List<ProductReview> reviewList;
    private SimpleDateFormat dateFormat;

    public ProductReviewAdapter(List<ProductReview> reviewList) {
        this.reviewList = reviewList;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        ProductReview review = reviewList.get(position);

        holder.tvUserName.setText(review.getUserName() != null ? review.getUserName() : "Anonymous");
        holder.ratingBar.setRating(review.getRating());
        holder.tvComment.setText(review.getComment() != null ? review.getComment() : "");

        // Format date
        String displayDate;
        if (review.getTimestamp() != null) {
            displayDate = dateFormat.format(review.getTimestamp());
        } else {
            displayDate = "Just now";
        }
        holder.tvDate.setText(displayDate);

        // Show rating text
        holder.tvRatingText.setText(String.format(Locale.getDefault(), "%.1f", review.getRating()));
    }

    @Override
    public int getItemCount() {
        return reviewList != null ? reviewList.size() : 0;
    }

    public void updateReviews(List<ProductReview> newReviews) {
        reviewList.clear();
        reviewList.addAll(newReviews);
        notifyDataSetChanged();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName;
        RatingBar ratingBar;
        TextView tvComment;
        TextView tvDate;
        TextView tvRatingText;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvRatingText = itemView.findViewById(R.id.tvRatingText);
        }
    }
}