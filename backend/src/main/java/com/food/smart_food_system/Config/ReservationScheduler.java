package com.food.smart_food_system.Config;

import com.food.smart_food_system.Service.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tác vụ định kỳ: tự động hủy các lượt đặt bàn đã tạo link đặt cọc
 * nhưng khách không thanh toán trong thời gian quy định (mặc định 3 phút).
 *
 * Cấu hình trong application.properties:
 *   app.reservation.payment-timeout-minutes   (mặc định 3)
 *   app.reservation.timeout-check-interval-ms  (mặc định 20000 = 20 giây)
 */
@Component
public class ReservationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationScheduler.class);

    private final ReservationService reservationService;

    @Value("${app.reservation.payment-timeout-minutes:3}")
    private int paymentTimeoutMinutes;

    public ReservationScheduler(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(fixedDelayString = "${app.reservation.timeout-check-interval-ms:20000}")
    public void autoCancelExpiredDeposits() {
        try {
            int cancelled = reservationService.autoCancelExpiredDepositReservations(paymentTimeoutMinutes);
            if (cancelled > 0) {
                log.info("[Scheduler] Đã tự hủy {} lượt đặt bàn quá hạn thanh toán cọc", cancelled);
            }
        } catch (Exception e) {
            log.error("[Scheduler] Lỗi khi tự hủy đặt bàn quá hạn thanh toán", e);
        }
    }
}
