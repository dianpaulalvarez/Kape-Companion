package com.lu.coffeecompanion.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;
import com.lu.coffeecompanion.models.OrderItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Order {
    private String orderId;
    private String userId;
    private String userName;
    private String name;
    private String address;
    private String mobile;
    private String paymentMethod;
    private String status;
    private String receiptNumber;
    private String proofImageUrl;
    private double totalPrice;
    private double deliveryFee;
    private Timestamp orderTimestamp;
    private long timestamp;
    private List<OrderItem> items;

    public Order() {
        this.items = new ArrayList<>();
        this.status = "Pending";
        this.timestamp = System.currentTimeMillis();
    }

    public Order(String orderId, String userId, String userName, String status,
                 double totalPrice, double deliveryFee, Timestamp orderTimestamp) {
        this.orderId = orderId;
        this.userId = userId;
        this.userName = userName;
        this.status = status;
        this.totalPrice = totalPrice;
        this.deliveryFee = deliveryFee;
        this.orderTimestamp = orderTimestamp;
        this.items = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    @PropertyName("userId")
    public String getUserId() {
        return userId;
    }

    @PropertyName("userId")
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @PropertyName("userName")
    public String getUserName() {
        return userName;
    }

    @PropertyName("userName")
    public void setUserName(String userName) {
        this.userName = userName;
    }

    @PropertyName("name")
    public String getName() {
        return name;
    }

    @PropertyName("name")
    public void setName(String name) {
        this.name = name;
    }

    @PropertyName("address")
    public String getAddress() {
        return address;
    }

    @PropertyName("address")
    public void setAddress(String address) {
        this.address = address;
    }

    @PropertyName("mobile")
    public String getMobile() {
        return mobile;
    }

    @PropertyName("mobile")
    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    @PropertyName("paymentMethod")
    public String getPaymentMethod() {
        return paymentMethod;
    }

    @PropertyName("paymentMethod")
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    @PropertyName("status")
    public String getStatus() {
        return status != null ? status : "Pending";
    }

    @PropertyName("status")
    public void setStatus(String status) {
        this.status = status;
    }

    @PropertyName("receiptNumber")
    public String getReceiptNumber() {
        return receiptNumber;
    }

    @PropertyName("receiptNumber")
    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    @PropertyName("proofImageUrl")
    public String getProofImageUrl() {
        return proofImageUrl;
    }

    @PropertyName("proofImageUrl")
    public void setProofImageUrl(String proofImageUrl) {
        this.proofImageUrl = proofImageUrl;
    }

    @PropertyName("totalPrice")
    public double getTotalPrice() {
        return totalPrice;
    }

    @PropertyName("totalPrice")
    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    @PropertyName("deliveryFee")
    public double getDeliveryFee() {
        return deliveryFee;
    }

    @PropertyName("deliveryFee")
    public void setDeliveryFee(double deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    @PropertyName("orderTimestamp")
    public Timestamp getOrderTimestamp() {
        return orderTimestamp;
    }

    @PropertyName("orderTimestamp")
    public void setOrderTimestamp(Timestamp orderTimestamp) {
        this.orderTimestamp = orderTimestamp;
    }

    @PropertyName("timestamp")
    public long getTimestamp() {
        return timestamp;
    }

    @PropertyName("timestamp")
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @PropertyName("items")
    public List<OrderItem> getItems() {
        return items;
    }

    @PropertyName("items")
    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    // Helper methods
    public double getTotalAmount() {
        return totalPrice + deliveryFee;
    }

    public String getFormattedDate() {
        if (orderTimestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(orderTimestamp.toDate());
        } else if (timestamp > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
        return "Date not available";
    }

    public String getFormattedDateTime() {
        if (orderTimestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            return sdf.format(orderTimestamp.toDate());
        } else if (timestamp > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
        return "Date not available";
    }
}