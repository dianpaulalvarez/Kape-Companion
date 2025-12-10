package com.lu.coffeecompanion.models;

import com.google.firebase.Timestamp;

public class OrderModel {
    private String orderId;
    private String userId;
    private String name;
    private String address;
    private String mobile;
    private double totalPrice;
    private Timestamp orderTimestamp;
    private String status;
    private String paymentMethod;
    private String proofImageUrl;

    public OrderModel() {}

    // getters and setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public Timestamp getOrderTimestamp() { return orderTimestamp; }
    public void setOrderTimestamp(Timestamp orderTimestamp) { this.orderTimestamp = orderTimestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getProofImageUrl() { return proofImageUrl; }
    public void setProofImageUrl(String proofImageUrl) { this.proofImageUrl = proofImageUrl; }
}