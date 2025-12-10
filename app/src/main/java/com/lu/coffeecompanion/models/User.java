package com.lu.coffeecompanion.models;

public class User {
    private String id;
    private String name;
    private String email;
    private String mobile;
    private String role;
    private Boolean blocked;

    public User() {
        // Required empty constructor for Firebase
    }

    public User(String id, String name, String email, String mobile, String role, Boolean blocked) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.mobile = mobile;
        this.role = role;
        this.blocked = blocked;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getBlocked() {
        return blocked;
    }

    public void setBlocked(Boolean blocked) {
        this.blocked = blocked;
    }

    public boolean isBlocked() {
        return blocked != null && blocked;
    }
}