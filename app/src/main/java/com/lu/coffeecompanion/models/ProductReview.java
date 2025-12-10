package com.lu.coffeecompanion.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProductReview {
    private String id;
    private String productId;
    private String userId;
    private String userName;
    private String orderId;
    private float rating;
    private String comment;
    private Date timestamp;

    // Add this method
    public String getFormattedDate() {
        if (timestamp == null) {
            return "Just now";
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(timestamp);
        } catch (Exception e) {
            return "Recent";
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}