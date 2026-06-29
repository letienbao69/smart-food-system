package com.food.smart_food_system.Repository;

import com.food.smart_food_system.Entity.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
    List<ReviewEntity> findByFoodId(Long foodId);
    void deleteByOrderId(Long orderId);

    @Query("SELECT r FROM ReviewEntity r WHERE r.rating >= 5 AND r.comment IS NOT NULL AND LENGTH(TRIM(r.comment)) > 0 ORDER BY r.createdAt DESC")
    List<ReviewEntity> findTestimonials();
}
