package com.food.smart_food_system.Repository;

import com.food.smart_food_system.Entity.OrderStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistoryEntity, Long> {

    void deleteByOrderId(Long orderId);
}
