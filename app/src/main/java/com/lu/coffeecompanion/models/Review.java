package com.lu.coffeecompanion.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

public class Review {
    private String reviewId;
    private String orderId;
    private String menuItemId;
    private String itemName;
    private String userId;
    private String userName;
    private float rating;
    private String feedback;
    private Timestamp createdAt;

    public Review() {
    }

    public Review(String orderId, String menuItemId, String itemName, String userId,
                  String userName, float rating, String feedback) {
        this.orderId = orderId;
        this.menuItemId = menuItemId;
        this.itemName = itemName;
        this.userId = userId;
        this.userName = userName;
        this.rating = rating;
        this.feedback = feedback;
        this.createdAt = Timestamp.now();
    }

    @PropertyName("reviewId")
    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }

    @PropertyName("orderId")
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    @PropertyName("menuItemId")
    public String getMenuItemId() { return menuItemId; }
    public void setMenuItemId(String menuItemId) { this.menuItemId = menuItemId; }

    @PropertyName("itemName")
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    @PropertyName("userId")
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @PropertyName("userName")
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    @PropertyName("rating")
    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    @PropertyName("feedback")
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    @PropertyName("createdAt")
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}