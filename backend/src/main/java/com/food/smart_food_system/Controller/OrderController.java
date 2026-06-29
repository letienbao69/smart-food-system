package com.food.smart_food_system.Controller;

import com.food.smart_food_system.DTO.OrderResponseDTO;
import com.food.smart_food_system.DTO.UpdateOrderStatusRequest;
import com.food.smart_food_system.Reponse.ApiResponse;
import com.food.smart_food_system.Service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Đơn món trong mô hình ăn tại nhà hàng. Việc tạo đơn món nằm ở luồng
 * ĐẶT BÀN (đặt món trước) — xem ReservationController. Controller này chỉ
 * dùng để xem / cập nhật trạng thái / xóa đơn món.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<OrderResponseDTO>>> getMy(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getMyOrders(authentication.getName())));
    }

    @GetMapping("/my/{id}")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> detail(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getMyOrderDetail(authentication.getName(), id)));
    }

    @DeleteMapping("/my/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMyOrder(Authentication authentication, @PathVariable Long id) {
        service.deleteMyOrder(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Đã xóa đơn món", null));
    }

    // ── ADMIN ──
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getDetailForAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getOrderDetailForAdmin(id)));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponseDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getAllOrders()));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> updateStatus(
            @PathVariable Long id, @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái đơn món thành công", service.updateStatus(id, request)));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable Long id) {
        service.deleteOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa đơn món", null));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PostMapping("/by-reservation/{reservationId}/items")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> addItemByReservation(
            @PathVariable Long reservationId, @RequestBody java.util.Map<String, Object> body) {
        Long foodId = Long.valueOf(String.valueOf(body.get("foodId")));
        Integer qty = body.get("quantity") == null ? 1 : Integer.valueOf(String.valueOf(body.get("quantity")));
        return ResponseEntity.ok(ApiResponse.success("Đã thêm món", service.addItemByReservation(reservationId, foodId, qty)));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PostMapping("/by-reservation/{reservationId}/voucher")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> applyVoucherByReservation(
            @PathVariable Long reservationId, @RequestBody java.util.Map<String, Object> body) {
        String code = body.get("voucherCode") == null ? null : String.valueOf(body.get("voucherCode"));
        OrderResponseDTO dto = service.applyVoucherByReservation(reservationId, code);
        String msg = (code == null || code.isBlank()) ? "Đã bỏ mã giảm giá" : "Đã áp dụng mã giảm giá";
        return ResponseEntity.ok(ApiResponse.success(msg, dto));
    }

    // ── Sửa món trong đơn (nhân viên/admin) ──
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PostMapping("/{id}/items")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> addItem(
            @PathVariable Long id, @RequestBody java.util.Map<String, Object> body) {
        Long foodId = Long.valueOf(String.valueOf(body.get("foodId")));
        Integer qty = body.get("quantity") == null ? 1 : Integer.valueOf(String.valueOf(body.get("quantity")));
        return ResponseEntity.ok(ApiResponse.success("Đã thêm món", service.addItemToOrder(id, foodId, qty)));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PutMapping("/{id}/items/{itemId}")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> updateItem(
            @PathVariable Long id, @PathVariable Long itemId, @RequestBody java.util.Map<String, Object> body) {
        Integer qty = Integer.valueOf(String.valueOf(body.get("quantity")));
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật món", service.updateOrderItem(id, itemId, qty)));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> removeItem(
            @PathVariable Long id, @PathVariable Long itemId) {
        return ResponseEntity.ok(ApiResponse.success("Đã xóa món", service.removeOrderItem(id, itemId)));
    }
}
