    package com.lu.coffeecompanion.models;
    ;

    public class PaymentProof {
        private String orderId;
        private String proofImageUrl;
        private String customerName;
        private double totalPrice;
        private String paymentMethod;

        // Default constructor required for Firestore
        public PaymentProof() {}

        // Getters and setters
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }

        public String getProofImageUrl() { return proofImageUrl; }
        public void setProofImageUrl(String proofImageUrl) { this.proofImageUrl = proofImageUrl; }

        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }

        public double getTotalPrice() { return totalPrice; }
        public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    }