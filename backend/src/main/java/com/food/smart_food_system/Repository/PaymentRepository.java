package com.food.smart_food_system.Repository;

import com.food.smart_food_system.DTO.PaymentStatisticDTO;
import com.food.smart_food_system.Entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    void deleteByOrderId(Long orderId);

    @Query("""
        SELECT new com.food.smart_food_system.DTO.PaymentStatisticDTO(
            p.status,
            COUNT(p.id),
            SUM(p.amount)
        )
        FROM PaymentEntity p
        WHERE p.createdAt BETWEEN :startDate AND :endDate
        GROUP BY p.status
    """)
    List<PaymentStatisticDTO> getPaymentStatistics(LocalDateTime startDate, LocalDateTime endDate);
}
