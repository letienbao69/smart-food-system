package com.food.smart_food_system.Repository;

import com.food.smart_food_system.Entity.CartEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<CartEntity, Long> {
    Optional<CartEntity> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}