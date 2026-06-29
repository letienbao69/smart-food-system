package com.food.smart_food_system.Controller;

import com.food.smart_food_system.DTO.LoginRequest;
import com.food.smart_food_system.Reponse.ApiResponse;
import com.food.smart_food_system.Reponse.AuthResponse;
import com.food.smart_food_system.Service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", authService.login(request)));
    }
}
