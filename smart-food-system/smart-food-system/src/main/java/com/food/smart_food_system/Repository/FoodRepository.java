package com.food.smart_food_system.Repository;

import com.food.smart_food_system.Entity.FoodEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodRepository extends JpaRepository<FoodEntity, Long> {
    List<FoodEntity> findByCategoryId(Long categoryId);
    List<FoodEntity> findByNameContainingIgnoreCase(String keyword);
    List<FoodEntity> findByStatus(String status);
    List<FoodEntity> findByCategoryIdAndStatus(Long categoryId, String status);
}