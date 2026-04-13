package com.food.smart_food_system.Repository;

import com.food.smart_food_system.Entity.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItemEntity, Long> {
    List<CartItemEntity> findByCartId(Long cartId);
    Optional<CartItemEntity> findByCartIdAndFoodId(Long cartId, Long foodId);
    void deleteByCartId(Long cartId);
}