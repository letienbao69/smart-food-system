package com.food.smart_food_system.Controller;

import com.food.smart_food_system.DTO.ForgotPasswordRequest;
import com.food.smart_food_system.DTO.ResetPasswordRequest;
import com.food.smart_food_system.Reponse.ApiResponse;
import com.food.smart_food_system.Service.PasswordResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tạo yêu cầu quên mật khẩu thành công",
                passwordResetService.forgotPassword(request)
        ));
    }

    @GetMapping("/reset-password/verify")
    public ResponseEntity<?> verifyResetToken(@RequestParam String token) {
        return ResponseEntity.ok(ApiResponse.success(
                "Kiểm tra token thành công",
                passwordResetService.verifyToken(token)
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);

        return ResponseEntity.ok(ApiResponse.success(
                "Đặt lại mật khẩu thành công",
                null
        ));
    }
}