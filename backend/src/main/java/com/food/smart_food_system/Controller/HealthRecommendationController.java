package com.food.smart_food_system.Controller;

import com.food.smart_food_system.DTO.health.HealthAnalysisDTO;
import com.food.smart_food_system.DTO.health.HealthProfileResponseDTO;
import com.food.smart_food_system.DTO.health.HealthRecommendationResponseDTO;
import com.food.smart_food_system.DTO.health.UpdateHealthProfileRequest;
import com.food.smart_food_system.Reponse.ApiResponse;
import com.food.smart_food_system.Service.HealthRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Health profile + BMI-based food recommendation.
 *
 * This is the AI feature defined in the project outline:
 *   "Tích hợp chức năng gợi ý món ăn thông minh dựa trên chỉ số BMI và tình trạng
 *    sức khỏe của người dùng"
 *
 * All endpoints operate on the currently-authenticated user only. There is no
 * userId path parameter - we resolve identity from the JWT to avoid IDOR.
 */
@Tag(name = "Health & BMI Recommendation",
        description = "Hồ sơ sức khỏe và gợi ý món ăn theo BMI / tình trạng dinh dưỡng")
@RestController
@RequestMapping("/api/health")
public class HealthRecommendationController {

    private final HealthRecommendationService service;

    public HealthRecommendationController(HealthRecommendationService service) {
        this.service = service;
    }

    @Operation(summary = "Lấy hồ sơ sức khỏe của người dùng hiện tại")
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication auth) {
        HealthProfileResponseDTO profile = service.getProfile(auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Lấy hồ sơ sức khỏe thành công", profile));
    }

    @Operation(summary = "Cập nhật hồ sơ sức khỏe (chiều cao, cân nặng, bệnh nền, mục tiêu...)")
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(Authentication auth,
                                           @Valid @RequestBody UpdateHealthProfileRequest req) {
        HealthProfileResponseDTO updated = service.updateProfile(auth.getName(), req);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ sức khỏe thành công", updated));
    }

    @Operation(summary = "Phân tích BMI / TDEE và đưa ra khẩu phần kcal phù hợp")
    @GetMapping("/analysis")
    public ResponseEntity<?> getAnalysis(Authentication auth) {
        HealthAnalysisDTO analysis = service.analyze(auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Phân tích sức khỏe thành công", analysis));
    }

    @Operation(summary = "Gợi ý món ăn dựa trên hồ sơ sức khỏe (BMI, dinh dưỡng, bệnh nền)")
    @GetMapping("/recommendations")
    public ResponseEntity<?> getRecommendations(
            Authentication auth,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(name = "useAi", defaultValue = "false") boolean useAi) {
        HealthRecommendationResponseDTO result = service.recommend(auth.getName(), limit, useAi);
        return ResponseEntity.ok(ApiResponse.success("Gợi ý món ăn theo sức khỏe thành công", result));
    }
}
