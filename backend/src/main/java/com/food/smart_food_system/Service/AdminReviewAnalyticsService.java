package com.food.smart_food_system.Service;

import com.food.smart_food_system.DTO.ReviewAnalyticsDTO;
import com.food.smart_food_system.Entity.ReviewEntity;
import com.food.smart_food_system.Repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminReviewAnalyticsService {

    private final ReviewRepository reviewRepository;

    public AdminReviewAnalyticsService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public ReviewAnalyticsDTO getAnalytics() {
        List<ReviewEntity> reviews = reviewRepository.findAll();

        long total = reviews.size();

        long positive = reviews.stream()
                .filter(review -> "POSITIVE".equalsIgnoreCase(review.getSentimentLabel()))
                .count();

        long negative = reviews.stream()
                .filter(review -> "NEGATIVE".equalsIgnoreCase(review.getSentimentLabel()))
                .count();

        long neutral = reviews.stream()
                .filter(review -> review.getSentimentLabel() == null
                        || "NEUTRAL".equalsIgnoreCase(review.getSentimentLabel()))
                .count();

        double averageRating = reviews.stream()
                .mapToInt(ReviewEntity::getRating)
                .average()
                .orElse(0);

        ReviewAnalyticsDTO dto = new ReviewAnalyticsDTO();
        dto.setTotalReviews(total);
        dto.setPositive(positive);
        dto.setNegative(negative);
        dto.setNeutral(neutral);

        dto.setPositiveRate(rate(positive, total));
        dto.setNegativeRate(rate(negative, total));
        dto.setNeutralRate(rate(neutral, total));
        dto.setAverageRating(round2(averageRating));

        return dto;
    }

    private double rate(long value, long total) {
        if (total == 0) {
            return 0;
        }

        return round2((value * 100.0) / total);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}