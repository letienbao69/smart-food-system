package com.food.smart_food_system.Repository;

import com.food.smart_food_system.Entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Optional<OrderEntity> findByOrderCode(String orderCode);
    List<OrderEntity> findByUserId(Long userId);
    List<OrderEntity> findByOrderStatus(String orderStatus);
    List<OrderEntity> findByPaymentStatus(String paymentStatus);
}