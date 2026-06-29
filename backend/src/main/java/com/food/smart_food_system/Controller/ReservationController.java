package com.food.smart_food_system.Controller;

import com.food.smart_food_system.DTO.CreateReservationRequest;
import com.food.smart_food_system.DTO.ReservationResponseDTO;
import com.food.smart_food_system.DTO.UpdateReservationStatusRequest;
import com.food.smart_food_system.Reponse.ApiResponse;
import com.food.smart_food_system.Service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    // ── Khách hàng ──
    @PostMapping
    public ResponseEntity<ApiResponse<ReservationResponseDTO>> create(
            Authentication auth, @Valid @RequestBody CreateReservationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đặt bàn thành công", service.createReservation(auth.getName(), request)));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ReservationResponseDTO>>> getMy(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getMyReservations(auth.getName())));
    }

    @GetMapping("/my/{id}")
    public ResponseEntity<ApiResponse<ReservationResponseDTO>> myDetail(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getMyReservationDetail(auth.getName(), id)));
    }

    @PostMapping("/my/{id}/cancel")
    public ResponseEntity<ApiResponse<ReservationResponseDTO>> cancel(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã hủy đặt bàn", service.cancelMyReservation(auth.getName(), id)));
    }

    /** Khách báo đã chuyển khoản tiền cọc qua QR -> admin xác nhận. */
    @PostMapping("/my/{id}/notify-deposit")
    public ResponseEntity<ApiResponse<ReservationResponseDTO>> notifyDeposit(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã gửi thông báo chuyển khoản cọc, vui lòng chờ xác nhận",
                service.notifyDepositPaid(auth.getName(), id)));
    }

    // ── Admin ──
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReservationResponseDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getAll()));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReservationResponseDTO>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getDetailForAdmin(id)));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ReservationResponseDTO>> updateStatus(
            @PathVariable Long id, @RequestBody UpdateReservationStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật đặt bàn thành công", service.updateStatus(id, request)));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.deleteReservation(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa lượt đặt bàn", null));
    }
}
