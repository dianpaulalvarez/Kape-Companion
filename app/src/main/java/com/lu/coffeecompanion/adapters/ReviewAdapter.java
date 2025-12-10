package com.lu.coffeecompanion.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.lu.coffeecompanion.R;
import com.lu.coffeecompanion.models.Review;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    private final List<DocumentSnapshot> reviewDocuments = new ArrayList<>();
    private final List<Review> reviewList = new ArrayList<>();
    private Context context;
    private boolean useDocuments = true; // Flag to determine which data source to use

    // Constructor with Context (for DocumentSnapshots)
    public ReviewAdapter(Context context) {
        this.context = context;
        this.useDocuments = true;
    }

    // Constructor with Review List
    public ReviewAdapter(List<Review> reviewList) {
        this.reviewList.addAll(reviewList);
        this.useDocuments = false;
    }

    // Constructor with Context and Review List
    public ReviewAdapter(Context context, List<Review> reviewList) {
        this.context = context;
        this.reviewList.addAll(reviewList);
        this.useDocuments = false;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (useDocuments) {
            bindDocumentSnapshot(holder, position);
        } else {
            bindReviewObject(holder, position);
        }
    }

    // Bind data from DocumentSnapshot
    private void bindDocumentSnapshot(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot doc = reviewDocuments.get(position);

        try {
            // Get user name
            String userName = doc.getString("userName");
            if (userName != null && !userName.isEmpty()) {
                holder.reviewUserName.setText(userName);
            } else {
                holder.reviewUserName.setText("Anonymous");
            }

            // Get item name (if available)
            String itemName = doc.getString("itemName");
            if (itemName != null && !itemName.isEmpty() && holder.reviewItemName != null) {
                holder.reviewItemName.setText(itemName);
                holder.reviewItemName.setVisibility(View.VISIBLE);
            } else if (holder.reviewItemName != null) {
                holder.reviewItemName.setVisibility(View.GONE);
            }

            // Get rating
            Double rating = doc.getDouble("rating");
            if (rating != null) {
                holder.reviewRating.setRating(rating.floatValue());
            } else {
                holder.reviewRating.setRating(0);
            }

            // Get feedback (comment)
            String feedback = doc.getString("feedback");
            if (feedback == null) {
                feedback = doc.getString("comment"); // Fallback to comment field
            }

            if (feedback != null && !feedback.isEmpty()) {
                holder.reviewFeedback.setText(feedback);
            } else {
                holder.reviewFeedback.setText("No feedback provided");
            }

            // Get date from timestamp
            Timestamp timestamp = doc.getTimestamp("createdAt");
            if (timestamp == null) {
                timestamp = doc.getTimestamp("timestamp"); // Fallback
            }

            if (timestamp != null) {
                Date date = timestamp.toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                holder.reviewDate.setText(sdf.format(date));
            } else {
                holder.reviewDate.setText("");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Bind data from Review object
    private void bindReviewObject(@NonNull ViewHolder holder, int position) {
        Review review = reviewList.get(position);

        try {
            // Get user name
            String userName = review.getUserName();
            if (userName != null && !userName.isEmpty()) {
                holder.reviewUserName.setText(userName);
            } else {
                holder.reviewUserName.setText("Anonymous");
            }

            // Get item name
            String itemName = review.getItemName();
            if (itemName != null && !itemName.isEmpty() && holder.reviewItemName != null) {
                holder.reviewItemName.setText(itemName);
                holder.reviewItemName.setVisibility(View.VISIBLE);
            } else if (holder.reviewItemName != null) {
                holder.reviewItemName.setVisibility(View.GONE);
            }

            // Get rating
            holder.reviewRating.setRating(review.getRating());

            // Get feedback
            String feedback = review.getFeedback();
            if (feedback != null && !feedback.isEmpty()) {
                holder.reviewFeedback.setText(feedback);
            } else {
                holder.reviewFeedback.setText("No feedback provided");
            }

            // Get date
            Timestamp timestamp = review.getCreatedAt();
            if (timestamp != null) {
                Date date = timestamp.toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                holder.reviewDate.setText(sdf.format(date));
            } else {
                holder.reviewDate.setText("");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return useDocuments ? reviewDocuments.size() : reviewList.size();
    }

    // Set reviews from Firestore documents
    public void setReviews(List<DocumentSnapshot> documents) {
        reviewDocuments.clear();
        if (documents != null) {
            reviewDocuments.addAll(documents);
        }
        useDocuments = true;
        notifyDataSetChanged();
    }

    // Set reviews from Review objects
    public void setReviewList(List<Review> reviews) {
        reviewList.clear();
        if (reviews != null) {
            reviewList.addAll(reviews);
        }
        useDocuments = false;
        notifyDataSetChanged();
    }

    // Add single review
    public void addReview(Review review) {
        if (review != null) {
            reviewList.add(0, review); // Add to top
            notifyItemInserted(0);
        }
    }

    // Clear all reviews
    public void clear() {
        reviewDocuments.clear();
        reviewList.clear();
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView reviewUserName;
        TextView reviewItemName;
        RatingBar reviewRating;
        TextView reviewFeedback;
        TextView reviewDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Try to find views with both possible ID names
            reviewUserName = itemView.findViewById(R.id.reviewUserName);
            if (reviewUserName == null) {
                reviewUserName = itemView.findViewById(R.id.reviewUserName);
            }

            reviewItemName = itemView.findViewById(R.id.review_item_name);
            if (reviewItemName == null) {
                reviewItemName = itemView.findViewById(R.id.review_item_name);
            }

            reviewRating = itemView.findViewById(R.id.reviewRating);
            if (reviewRating == null) {
                reviewRating = itemView.findViewById(R.id.reviewRating);
            }

            reviewFeedback = itemView.findViewById(R.id.review_feedback);
            if (reviewFeedback == null) {
                reviewFeedback = itemView.findViewById(R.id.review_feedback);
            }

            reviewDate = itemView.findViewById(R.id.tvReviewDate);
            if (reviewDate == null) {
                reviewDate = itemView.findViewById(R.id.tvReviewDate);
            }
        }
    }
}