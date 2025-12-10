package com.lu.coffeecompanion;

public class CartItem {
    private String cartId; // Firestore cart document ID
    private String name;
    private double price;
    private String shop;
    private int quantity;
    private String imageUrl;
    private boolean isSelected;

    public CartItem(String cartId, String name, double price, String shop, int quantity, String imageUrl) {
        this.cartId = cartId;
        this.name = name;
        this.price = price;
        this.shop = shop;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
        this.isSelected = false;
    }

    // GETTERS
    public String getCartId() { return cartId; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getShop() { return shop; }
    public int getQuantity() { return quantity; }
    public String getImageUrl() { return imageUrl; }
    public boolean isSelected() { return isSelected; }

    // SETTERS
    public void setSelected(boolean selected) { isSelected = selected; }

    public void setQuantity(int quantity) {
        if (quantity < 1) quantity = 1; // ensure minimum quantity is 1
        this.quantity = quantity;
    }

    // HELPER METHODS
    public void incrementQuantity() { this.quantity += 1; }
    public void decrementQuantity() { if (this.quantity > 1) this.quantity -= 1; }
}
