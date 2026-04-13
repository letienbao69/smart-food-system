package com.food.smart_food_system.Repository;

import com.food.smart_food_system.Entity.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
    List<ReviewEntity> findByFoodId(Long foodId);
    List<ReviewEntity> findByUserId(Long userId);
    boolean existsByUserIdAndFoodIdAndOrderId(Long userId, Long foodId, Long orderId);
}