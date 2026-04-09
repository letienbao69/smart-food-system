package com.food.smart_food_system.Reponse;

public class AuthResponse {
    private String token;
    private String email;
    private String role;
    private String full_name;
    private String phone;
    private String status;

    public AuthResponse(String token, String email, String full_name , String phone, String status) {
        this.token = token;
        this.email = email;
        this.full_name = full_name;
        this.phone = phone;
        this.status = status;
    }

    public String getToken() { return token; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getFullName() { return full_name; }
    public String getPhone() { return phone;}
    public String getStatus() { return status;}
}
