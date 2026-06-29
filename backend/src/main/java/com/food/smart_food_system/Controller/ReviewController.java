package com.food.smart_food_system.Controller;

import com.food.smart_food_system.DTO.CreateReviewRequest;
import com.food.smart_food_system.DTO.ReviewDTO;
import com.food.smart_food_system.Reponse.ApiResponse;
import com.food.smart_food_system.Service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService service;
    public ReviewController(ReviewService service) { this.service = service; }

    @PostMapping public ResponseEntity<ApiResponse<ReviewDTO>> create(Authentication authentication, @Valid @RequestBody CreateReviewRequest request) { return ResponseEntity.ok(ApiResponse.success("Đánh giá thành công", service.create(authentication.getName(), request))); }

    @GetMapping("/food/{foodId}") public ResponseEntity<ApiResponse<List<ReviewDTO>>> getByFood(@PathVariable Long foodId) { return ResponseEntity.ok(ApiResponse.success("OK", service.getByFood(foodId))); }

    @GetMapping("/testimonials") public ResponseEntity<ApiResponse<List<ReviewDTO>>> testimonials() { return ResponseEntity.ok(ApiResponse.success("OK", service.getTestimonials())); }
}
