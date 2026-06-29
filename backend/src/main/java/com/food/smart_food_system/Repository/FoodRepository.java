package com.food.smart_food_system.Repository;

import com.food.smart_food_system.Entity.FoodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface FoodRepository extends JpaRepository<FoodEntity, Long> {
    List<FoodEntity> findByCategoryId(Long categoryId);
    List<FoodEntity> findByNameContainingIgnoreCase(String keyword);

    @Query("SELECT f FROM FoodEntity f WHERE f.category.featured = true AND f.status = 'AVAILABLE' ORDER BY f.ratingAvg DESC, f.id DESC")
    List<FoodEntity> findFeatured();
}
