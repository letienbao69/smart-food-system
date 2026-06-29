package com.food.smart_food_system.Service;

import com.food.smart_food_system.DTO.OrderResponseDTO;
import com.food.smart_food_system.DTO.UpdateOrderStatusRequest;
import java.util.List;

/**
 * Dịch vụ ĐƠN MÓN trong mô hình ăn tại nhà hàng.
 * Đơn món được tạo từ luồng ĐẶT BÀN (đặt món trước) — xem ReservationService.
 * Service này chỉ phụ trách xem / cập nhật trạng thái / xóa đơn món.
 */
public interface OrderService {
    List<OrderResponseDTO> getMyOrders(String email);
    OrderResponseDTO getMyOrderDetail(String email, Long orderId);
    OrderResponseDTO getOrderDetailForAdmin(Long orderId);
    List<OrderResponseDTO> getAllOrders();

    /** Admin cập nhật trạng thái món (PENDING/CONFIRMED/PREPARING/SERVED/COMPLETED) và thanh toán. */
    OrderResponseDTO updateStatus(Long orderId, UpdateOrderStatusRequest request);

    /** Admin xóa đơn món (chỉ khi CANCELLED hoặc COMPLETED). */
    void deleteOrder(Long orderId);

    /** Khách hàng tự xóa đơn món của mình (chỉ khi CANCELLED hoặc COMPLETED). */
    void deleteMyOrder(Long orderId, String email);

    /** Nhân viên/Admin thêm món vào đơn (khi khách order tiếp tại bàn). */
    OrderResponseDTO addItemToOrder(Long orderId, Long foodId, Integer quantity);

    /** Thêm món theo lượt đặt bàn — nếu đặt bàn chưa có đơn món thì tự tạo đơn mới. */
    OrderResponseDTO addItemByReservation(Long reservationId, Long foodId, Integer quantity);
    OrderResponseDTO applyVoucherByReservation(Long reservationId, String voucherCode);

    /** Nhân viên/Admin đổi số lượng một món trong đơn. */
    OrderResponseDTO updateOrderItem(Long orderId, Long itemId, Integer quantity);

    /** Nhân viên/Admin xóa một món khỏi đơn. */
    OrderResponseDTO removeOrderItem(Long orderId, Long itemId);
}
