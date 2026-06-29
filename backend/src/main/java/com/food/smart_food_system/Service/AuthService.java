package com.food.smart_food_system.Service;

import com.food.smart_food_system.DTO.LoginRequest;
import com.food.smart_food_system.DTO.RegisterRequest;
import com.food.smart_food_system.Reponse.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse register(RegisterRequest request);
    AuthResponse me(String email);
}
