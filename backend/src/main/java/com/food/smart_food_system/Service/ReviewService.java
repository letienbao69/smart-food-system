package com.food.smart_food_system.Service;

import com.food.smart_food_system.DTO.CreateReviewRequest;
import com.food.smart_food_system.DTO.ReviewDTO;
import java.util.List;

public interface ReviewService {
    ReviewDTO create(String email, CreateReviewRequest request);
    List<ReviewDTO> getByFood(Long foodId);
    List<ReviewDTO> getTestimonials();
}
