package com.food.smart_food_system.Repository;

import com.food.smart_food_system.Entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    List<OrderEntity> findByUserIdOrderByIdDesc(Long userId);

    Optional<OrderEntity> findByIdAndUserId(Long id, Long userId);

    /** Đơn món đặt trước gắn với 1 lượt đặt bàn (mô hình ăn tại nhà hàng). */
    List<OrderEntity> findByReservationId(Long reservationId);

    Long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    Long countByOrderStatusAndCreatedAtBetween(
            String orderStatus,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    @Query("""
        SELECT SUM(o.finalAmount)
        FROM OrderEntity o
        WHERE o.createdAt BETWEEN :startDate AND :endDate
        AND o.orderStatus <> 'CANCELLED'
    """)
    BigDecimal sumRevenue(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
        SELECT SUM(o.finalAmount)
        FROM OrderEntity o
        WHERE o.paymentStatus = :paymentStatus
        AND o.createdAt BETWEEN :startDate AND :endDate
        AND o.orderStatus <> 'CANCELLED'
    """)
    BigDecimal sumRevenueByPaymentStatus(
            @Param("paymentStatus") String paymentStatus,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query(value = """
        SELECT 
            DATE(o.created_at) AS orderDate,
            COALESCE(SUM(o.final_amount), 0) AS revenue,
            COUNT(o.id) AS totalOrders
        FROM orders o
        WHERE o.created_at BETWEEN :startDate AND :endDate
        AND o.order_status <> 'CANCELLED'
        GROUP BY DATE(o.created_at)
        ORDER BY DATE(o.created_at)
    """, nativeQuery = true)
    List<Object[]> getDailyRevenueRaw(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    Optional<OrderEntity> findByOrderCode(String orderCode);

    /** Tìm đơn có orderCode xuất hiện trong nội dung chuyển khoản */
    @Query("SELECT o FROM OrderEntity o WHERE :content LIKE CONCAT('%', o.orderCode, '%')")
    List<OrderEntity> findByOrderCodeInContent(@Param("content") String content);
}
