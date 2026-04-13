package com.food.smart_food_system.DTO;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}