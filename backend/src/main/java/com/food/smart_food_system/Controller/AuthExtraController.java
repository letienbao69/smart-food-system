package com.food.smart_food_system.Controller;

import com.food.smart_food_system.DTO.RegisterRequest;
import com.food.smart_food_system.Reponse.ApiResponse;
import com.food.smart_food_system.Reponse.AuthResponse;
import com.food.smart_food_system.Service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthExtraController {
    private final AuthService authService;
    public AuthExtraController(AuthService authService) { this.authService = authService; }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đăng ký thành công", authService.register(request)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> me(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin thành công", authService.me(authentication.getName())));
    }
}
