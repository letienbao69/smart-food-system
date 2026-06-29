package com.food.smart_food_system.Repository;

import com.food.smart_food_system.DTO.BestSellingFoodDTO;
import com.food.smart_food_system.Entity.OrderItemEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {

    List<OrderItemEntity> findByOrderId(Long orderId);

    void deleteByOrderId(Long orderId);

    boolean existsByFoodId(Long foodId);

    @Query("""
        SELECT new com.food.smart_food_system.DTO.BestSellingFoodDTO(
            oi.food.id,
            oi.foodName,
            SUM(oi.quantity),
            SUM(oi.subtotal)
        )
        FROM OrderItemEntity oi
        WHERE oi.order.createdAt BETWEEN :startDate AND :endDate
        AND oi.order.orderStatus <> 'CANCELLED'
        GROUP BY oi.food.id, oi.foodName
        ORDER BY SUM(oi.quantity) DESC
    """)
    List<BestSellingFoodDTO> getBestSellingFoods(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );
}
