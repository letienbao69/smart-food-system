package com.food.smart_food_system.Repository;

import com.food.smart_food_system.Entity.RestaurantTableEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTableEntity, Long> {
    Optional<RestaurantTableEntity> findByTableNumber(String tableNumber);
    boolean existsByTableNumber(String tableNumber);
    List<RestaurantTableEntity> findByStatusOrderByTableNumberAsc(String status);
    List<RestaurantTableEntity> findAllByOrderByTableNumberAsc();
    List<RestaurantTableEntity> findByCapacityGreaterThanEqualAndStatusOrderByCapacityAsc(Integer capacity, String status);
}
