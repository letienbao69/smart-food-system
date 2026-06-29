package com.food.smart_food_system.Service.impl;

import com.food.smart_food_system.DTO.CreateReviewRequest;
import com.food.smart_food_system.DTO.ReviewDTO;
import com.food.smart_food_system.Entity.FoodEntity;
import com.food.smart_food_system.Entity.OrderEntity;
import com.food.smart_food_system.Entity.ReviewEntity;
import com.food.smart_food_system.Entity.UserEntity;
import com.food.smart_food_system.Exception.BusinessException;
import com.food.smart_food_system.Exception.ResourceNotFoundException;
import com.food.smart_food_system.Repository.FoodRepository;
import com.food.smart_food_system.Repository.OrderRepository;
import com.food.smart_food_system.Repository.ReviewRepository;
import com.food.smart_food_system.Service.CustomUserDetailsService;
import com.food.smart_food_system.Service.ReviewSentimentService;
import com.food.smart_food_system.Service.ReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final FoodRepository foodRepository;
    private final OrderRepository orderRepository;
    private final CustomUserDetailsService customUserDetailsService;
    private final ReviewSentimentService reviewSentimentService;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             FoodRepository foodRepository,
                             OrderRepository orderRepository,
                             CustomUserDetailsService customUserDetailsService,
                             ReviewSentimentService reviewSentimentService) {
        this.reviewRepository = reviewRepository;
        this.foodRepository = foodRepository;
        this.orderRepository = orderRepository;
        this.customUserDetailsService = customUserDetailsService;
        this.reviewSentimentService = reviewSentimentService;
    }

    @Override
    public ReviewDTO create(String email, CreateReviewRequest request) {
        UserEntity user = customUserDetailsService.getUserByEmail(email);

        FoodEntity food = foodRepository.findById(request.getFoodId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy món ăn"));

        OrderEntity order = orderRepository.findByIdAndUserId(request.getOrderId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        if (!"COMPLETED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new BusinessException("Chỉ được đánh giá đơn hàng đã hoàn thành");
        }

        ReviewEntity entity = new ReviewEntity();
        entity.setUser(user);
        entity.setFood(food);
        entity.setOrder(order);
        entity.setRating(request.getRating());
        entity.setComment(request.getComment());

        // Tự động phân tích cảm xúc, không lấy từ request nữa
        entity.setSentimentLabel(
                reviewSentimentService.analyze(request.getComment(), request.getRating())
        );

        reviewRepository.save(entity);

        List<ReviewEntity> reviews = reviewRepository.findByFoodId(food.getId());
        BigDecimal avg = BigDecimal.valueOf(
                reviews.stream().mapToInt(ReviewEntity::getRating).average().orElse(0)
        ).setScale(2, RoundingMode.HALF_UP);

        food.setRatingAvg(avg);
        foodRepository.save(food);

        return toDto(entity);
    }

    @Override
    public List<ReviewDTO> getByFood(Long foodId) {
        return reviewRepository.findByFoodId(foodId).stream().map(this::toDto).toList();
    }

    @Override
    public List<ReviewDTO> getTestimonials() {
        return reviewRepository.findTestimonials().stream().map(this::toDto).toList();
    }

    private ReviewDTO toDto(ReviewEntity e) {
        ReviewDTO dto = new ReviewDTO();

        dto.setId(e.getId());
        dto.setUserId(e.getUser().getId());
        dto.setUserName(e.getUser().getFullName());
        dto.setFoodId(e.getFood().getId());
        dto.setFoodName(e.getFood().getName());
        dto.setOrderId(e.getOrder().getId());
        dto.setRating(e.getRating());
        dto.setComment(e.getComment());
        dto.setSentimentLabel(e.getSentimentLabel());
        dto.setCreatedAt(e.getCreatedAt());

        return dto;
    }
}