package com.food.smart_food_system.Repository;

import com.food.smart_food_system.Entity.ReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<ReservationEntity, Long> {
    List<ReservationEntity> findByUserIdOrderByIdDesc(Long userId);
    Optional<ReservationEntity> findByIdAndUserId(Long id, Long userId);
    List<ReservationEntity> findAllByOrderByReservationTimeDesc();
    Optional<ReservationEntity> findByReservationCode(String reservationCode);

    /** Đặt bàn đang chiếm 1 bàn cụ thể trong khung giờ (để chặn trùng bàn). */
    List<ReservationEntity> findByTableIdAndStatusInAndReservationTimeBetween(
            Long tableId, List<String> statuses, LocalDateTime from, LocalDateTime to);

    boolean existsByTableIdAndStatus(Long tableId, String status);

    Long countByStatusAndReservationTimeBetween(String status, LocalDateTime from, LocalDateTime to);
    Long countByReservationTimeBetween(LocalDateTime from, LocalDateTime to);

    /** Đặt bàn còn chờ thanh toán cọc (đã tạo link) nhưng quá hạn → để tự hủy. */
    List<ReservationEntity> findByStatusAndDepositStatusNotAndDepositRequestedAtBefore(
            String status, String depositStatus, LocalDateTime cutoff);
}
