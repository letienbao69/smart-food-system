package com.food.smart_food_system.Service;

import com.food.smart_food_system.DTO.CreateReservationRequest;
import com.food.smart_food_system.DTO.ReservationResponseDTO;
import com.food.smart_food_system.DTO.UpdateReservationStatusRequest;

import java.util.List;

public interface ReservationService {
    // ── Khách hàng ──
    ReservationResponseDTO createReservation(String email, CreateReservationRequest request);
    List<ReservationResponseDTO> getMyReservations(String email);
    ReservationResponseDTO getMyReservationDetail(String email, Long id);
    ReservationResponseDTO cancelMyReservation(String email, Long id);
    /** Khách báo đã chuyển khoản tiền cọc qua QR -> chờ admin xác nhận. */
    ReservationResponseDTO notifyDepositPaid(String email, Long id);

    // Xác nhận đã nhận cọc qua cổng thanh toán (tự động từ webhook) + tự xác nhận đặt bàn
    ReservationResponseDTO markDepositPaidByGateway(Long id);
    // Tự động hủy các lượt đặt bàn quá hạn thanh toán cọc; trả về số lượt đã hủy
    int autoCancelExpiredDepositReservations(int timeoutMinutes);

    // ── Admin ──
    List<ReservationResponseDTO> getAll();
    ReservationResponseDTO getDetailForAdmin(Long id);
    ReservationResponseDTO updateStatus(Long id, UpdateReservationStatusRequest request);
    void deleteReservation(Long id);
}
