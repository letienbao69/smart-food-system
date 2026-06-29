package com.food.smart_food_system.Controller;

import com.food.smart_food_system.Reponse.ApiResponse;
import com.food.smart_food_system.Service.AdminReviewAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewAnalyticsController {

    private final AdminReviewAnalyticsService adminReviewAnalyticsService;

    public AdminReviewAnalyticsController(AdminReviewAnalyticsService adminReviewAnalyticsService) {
        this.adminReviewAnalyticsService = adminReviewAnalyticsService;
    }

    @GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics() {
        return ResponseEntity.ok(ApiResponse.success(
                "Phân tích phản hồi khách hàng thành công",
                adminReviewAnalyticsService.getAnalytics()
        ));
    }
}