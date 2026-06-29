package com.food.smart_food_system.Reponse;

import java.util.List;

public class AuthResponse {
    private String token;
    private String email;
    private String fullName;
    private String role;
    private String phone;
    private String avatarUrl;
    private List<String> roles;

    public AuthResponse() {}

    public AuthResponse(String token, String email, String fullName, String role) {
        this.token = token;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    public AuthResponse(String token, String email, String fullName, String role, String phone, String avatarUrl) {
        this.token = token;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
    }

    public AuthResponse(String token, String email, String fullName, String role, String phone, String avatarUrl, List<String> roles) {
        this.token = token;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
        this.roles = roles;
    }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
