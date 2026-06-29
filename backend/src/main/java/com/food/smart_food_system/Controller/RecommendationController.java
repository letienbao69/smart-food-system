package com.food.smart_food_system.Controller;

import com.food.smart_food_system.Reponse.ApiResponse;
import com.food.smart_food_system.Service.RecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/my")
    public ResponseEntity<?> recommendForMe(
            Authentication authentication,
            @RequestParam(defaultValue = "8") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Gợi ý món ăn cá nhân hóa thành công",
                recommendationService.recommendForCurrentUser(authentication.getName(), limit)
        ));
    }

    @GetMapping("/cart")
    public ResponseEntity<?> recommendForCart(
            Authentication authentication,
            @RequestParam(defaultValue = "8") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Gợi ý món ăn theo giỏ hàng thành công",
                recommendationService.recommendForCurrentCart(authentication.getName(), limit)
        ));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> recommendForUserByAdmin(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "8") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Admin lấy gợi ý món ăn theo user thành công",
                recommendationService.recommendForUserId(userId, limit)
        ));
    }
}