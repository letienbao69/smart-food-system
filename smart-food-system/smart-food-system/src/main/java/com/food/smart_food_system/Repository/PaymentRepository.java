package com.food.smart_food_system.Repository;

import com.food.smart_food_system.Entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    List<PaymentEntity> findByOrderId(Long orderId);
    Optional<PaymentEntity> findByTransactionCode(String transactionCode);
    List<PaymentEntity> findByStatus(String status);
}