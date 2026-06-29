package com.food.smart_food_system.Repository;

import com.food.smart_food_system.Entity.WishlistEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<WishlistEntity, Long> {

    List<WishlistEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<WishlistEntity> findByUserIdAndFoodId(Long userId, Long foodId);

    boolean existsByUserIdAndFoodId(Long userId, Long foodId);

    void deleteByUserIdAndFoodId(Long userId, Long foodId);

    long countByUserId(Long userId);
}