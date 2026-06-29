package com.food.smart_food_system.Service.impl;

import com.food.smart_food_system.DTO.OrderItemResponseDTO;
import com.food.smart_food_system.DTO.OrderResponseDTO;
import com.food.smart_food_system.DTO.UpdateOrderStatusRequest;
import com.food.smart_food_system.Entity.*;
import com.food.smart_food_system.Exception.BusinessException;
import com.food.smart_food_system.Exception.ResourceNotFoundException;
import com.food.smart_food_system.Repository.*;
import com.food.smart_food_system.Service.CustomUserDetailsService;
import com.food.smart_food_system.Service.NotificationService;
import com.food.smart_food_system.Service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final PaymentRepository paymentRepository;
    private final CustomUserDetailsService customUserDetailsService;
    private final NotificationService notificationService;
    private final ReviewRepository reviewRepository;
    private final FoodRepository foodRepository;
    private final ReservationRepository reservationRepository;
    private final com.food.smart_food_system.Repository.VoucherRepository voucherRepository;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            PaymentRepository paymentRepository,
            CustomUserDetailsService customUserDetailsService,
            NotificationService notificationService,
            ReviewRepository reviewRepository,
            FoodRepository foodRepository,
            ReservationRepository reservationRepository,
            com.food.smart_food_system.Repository.VoucherRepository voucherRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.paymentRepository = paymentRepository;
        this.customUserDetailsService = customUserDetailsService;
        this.notificationService = notificationService;
        this.reviewRepository = reviewRepository;
        this.foodRepository = foodRepository;
        this.reservationRepository = reservationRepository;
        this.voucherRepository = voucherRepository;
    }

    @Override
    public List<OrderResponseDTO> getMyOrders(String email) {
        UserEntity user = customUserDetailsService.getUserByEmail(email);
        return orderRepository.findByUserIdOrderByIdDesc(user.getId())
                .stream().map(this::toDto).toList();
    }

    @Override
    public OrderResponseDTO getMyOrderDetail(String email, Long orderId) {
        UserEntity user = customUserDetailsService.getUserByEmail(email);
        OrderEntity order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn món"));
        return toDto(order);
    }

    @Override
    public OrderResponseDTO getOrderDetailForAdmin(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn món"));
        return toDto(order);
    }

    @Override
    public List<OrderResponseDTO> getAllOrders() {
        return orderRepository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    public OrderResponseDTO updateStatus(Long orderId, UpdateOrderStatusRequest request) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn món"));

        String oldOrderStatus = order.getOrderStatus();
        String oldPaymentStatus = order.getPaymentStatus();

        String newOrderStatus = normalizeBlank(request.getOrderStatus());
        String newPaymentStatus = normalizeBlank(request.getPaymentStatus());

        if (newOrderStatus != null) {
            validateOrderStatus(newOrderStatus);
            // Đơn món chỉ COMPLETED khi đã thanh toán (tại quán)
            if ("COMPLETED".equals(newOrderStatus) && !"PAID".equalsIgnoreCase(order.getPaymentStatus())) {
                if (!"PAID".equalsIgnoreCase(newPaymentStatus)) {
                    throw new BusinessException(
                            "Không thể hoàn thành đơn món khi chưa thanh toán. Hãy xác nhận thanh toán trước (paymentStatus=PAID).");
                }
            }
            order.setOrderStatus(newOrderStatus);
        }

        if (newPaymentStatus != null) {
            validatePaymentStatus(newPaymentStatus);
            order.setPaymentStatus(newPaymentStatus);
        }

        if (request.getNote() != null) order.setNote(request.getNote());

        orderRepository.save(order);

        if (newOrderStatus != null && !newOrderStatus.equalsIgnoreCase(oldOrderStatus)) {
            OrderStatusHistoryEntity history = new OrderStatusHistoryEntity();
            history.setOrder(order);
            history.setStatus(newOrderStatus);
            history.setNote(request.getNote());
            orderStatusHistoryRepository.save(history);
            notificationService.notifyOrderStatusChanged(order, oldOrderStatus, request.getNote());
        }

        if (newPaymentStatus != null && !newPaymentStatus.equalsIgnoreCase(oldPaymentStatus)) {
            notificationService.notifyPaymentStatusChanged(order, oldPaymentStatus);
        }

        return toDto(order);
    }

    @Override
    public void deleteOrder(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn món"));
        if (!Set.of("CANCELLED", "COMPLETED").contains(order.getOrderStatus())) {
            throw new BusinessException("Chỉ có thể xóa đơn món đã hoàn thành hoặc đã hủy");
        }
        deleteOrderCascade(orderId);
    }

    @Override
    public void deleteMyOrder(Long orderId, String email) {
        UserEntity user = customUserDetailsService.getUserByEmail(email);
        OrderEntity order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn món"));
        if (!Set.of("CANCELLED", "COMPLETED").contains(order.getOrderStatus())) {
            throw new BusinessException("Chỉ có thể xóa đơn món đã hoàn thành hoặc đã hủy");
        }
        deleteOrderCascade(orderId);
    }

    private void deleteOrderCascade(Long orderId) {
        reviewRepository.deleteByOrderId(orderId);
        orderItemRepository.deleteByOrderId(orderId);
        orderStatusHistoryRepository.deleteByOrderId(orderId);
        paymentRepository.deleteByOrderId(orderId);
        orderRepository.deleteById(orderId);
    }

    // ───────────────────────────────────────────────────────────
    // SỬA MÓN TRONG ĐƠN (nhân viên/admin thực hiện khi khách order tiếp)
    // ───────────────────────────────────────────────────────────

    @Override
    public OrderResponseDTO addItemByReservation(Long reservationId, Long foodId, Integer quantity) {
        ReservationEntity rsv = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt đặt bàn"));
        // Tìm đơn món gắn với đặt bàn này; nếu chưa có -> tạo đơn rỗng
        OrderEntity order = orderRepository.findByReservationId(reservationId)
                .stream().findFirst().orElse(null);
        if (order == null) {
            order = new OrderEntity();
            order.setUser(rsv.getUser());
            order.setReservation(rsv);
            order.setOrderCode("ORD-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            order.setTotalAmount(java.math.BigDecimal.ZERO);
            order.setDiscountAmount(java.math.BigDecimal.ZERO);
            order.setFinalAmount(java.math.BigDecimal.ZERO);
            order.setPaymentMethod(rsv.getPaymentMethod() == null ? "CASH" : rsv.getPaymentMethod());
            order.setPaymentStatus("UNPAID");
            order.setOrderStatus("CONFIRMED");
            order.setNote("Đơn món gọi tại bàn cho " + rsv.getReservationCode());
            orderRepository.save(order);
            // Đánh dấu lượt đặt bàn đã có đơn món để hiển thị đúng ở phía quản trị
            if (!Boolean.TRUE.equals(rsv.getHasPreorder())) {
                rsv.setHasPreorder(true);
                reservationRepository.save(rsv);
            }
        }
        return addItemToOrder(order.getId(), foodId, quantity);
    }

    @Override
    public OrderResponseDTO applyVoucherByReservation(Long reservationId, String voucherCode) {
        OrderEntity order = orderRepository.findByReservationId(reservationId)
                .stream().findFirst()
                .orElseThrow(() -> new com.food.smart_food_system.Exception.BusinessException("Lượt đặt bàn chưa có món để áp mã"));
        java.util.List<OrderItemEntity> items = orderItemRepository.findByOrderId(order.getId());
        java.math.BigDecimal total = items.stream().map(OrderItemEntity::getSubtotal)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        if (voucherCode == null || voucherCode.isBlank()) {
            // Bỏ mã giảm giá
            order.setDiscountAmount(java.math.BigDecimal.ZERO);
            order.setFinalAmount(total);
            orderRepository.save(order);
            return toDto(order);
        }

        String code = voucherCode.trim().toUpperCase();
        var v = voucherRepository.findByCode(code)
                .orElseThrow(() -> new com.food.smart_food_system.Exception.BusinessException("Mã giảm giá không tồn tại"));

        // Kiểm tra hiệu lực
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (!"ACTIVE".equalsIgnoreCase(v.getStatus()))
            throw new com.food.smart_food_system.Exception.BusinessException("Mã giảm giá đã ngừng áp dụng");
        if (v.getStartDate() != null && now.isBefore(v.getStartDate()))
            throw new com.food.smart_food_system.Exception.BusinessException("Mã giảm giá chưa đến thời gian áp dụng");
        if (v.getEndDate() != null && now.isAfter(v.getEndDate()))
            throw new com.food.smart_food_system.Exception.BusinessException("Mã giảm giá đã hết hạn");
        if (v.getQuantity() != null && v.getQuantity() <= 0)
            throw new com.food.smart_food_system.Exception.BusinessException("Mã giảm giá đã hết lượt sử dụng");
        if (v.getMinOrderValue() != null && total.compareTo(v.getMinOrderValue()) < 0)
            throw new com.food.smart_food_system.Exception.BusinessException(
                    "Đơn tối thiểu " + v.getMinOrderValue().intValue() + "đ mới dùng được mã này");

        // Tính giảm
        java.math.BigDecimal discount;
        if (v.isPercent()) {
            discount = total.multiply(v.getDiscountValue()).divide(java.math.BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
            if (v.getMaxDiscount() != null && discount.compareTo(v.getMaxDiscount()) > 0) discount = v.getMaxDiscount();
        } else {
            discount = v.getDiscountValue();
        }
        if (discount.compareTo(total) > 0) discount = total;

        order.setTotalAmount(total);
        order.setDiscountAmount(discount);
        order.setFinalAmount(total.subtract(discount));
        orderRepository.save(order);
        return toDto(order);
    }

    @Override
    public OrderResponseDTO addItemToOrder(Long orderId, Long foodId, Integer quantity) {
        OrderEntity order = requireEditable(orderId);
        FoodEntity food = foodRepository.findById(foodId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy món ăn"));
        int qty = quantity == null || quantity < 1 ? 1 : quantity;

        // Giá đã áp khuyến mãi của món
        java.math.BigDecimal unit = priceAfterDiscount(food);

        // Nếu món đã có trong đơn -> cộng dồn số lượng
        List<OrderItemEntity> items = orderItemRepository.findByOrderId(orderId);
        OrderItemEntity existing = items.stream()
                .filter(i -> i.getFood() != null && i.getFood().getId().equals(foodId))
                .findFirst().orElse(null);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + qty);
            existing.setSubtotal(unit.multiply(java.math.BigDecimal.valueOf(existing.getQuantity())));
            orderItemRepository.save(existing);
        } else {
            OrderItemEntity item = new OrderItemEntity();
            item.setOrder(order);
            item.setFood(food);
            item.setFoodName(food.getName());
            item.setQuantity(qty);
            item.setUnitPrice(unit);
            item.setSubtotal(unit.multiply(java.math.BigDecimal.valueOf(qty)));
            orderItemRepository.save(item);
        }
        recalcOrder(order);
        return toDto(order);
    }

    @Override
    public OrderResponseDTO updateOrderItem(Long orderId, Long itemId, Integer quantity) {
        OrderEntity order = requireEditable(orderId);
        OrderItemEntity item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy món trong đơn"));
        if (item.getOrder() == null || !item.getOrder().getId().equals(orderId)) {
            throw new BusinessException("Món không thuộc đơn này");
        }
        int qty = quantity == null ? 0 : quantity;
        if (qty < 1) {
            orderItemRepository.delete(item);
        } else {
            item.setQuantity(qty);
            item.setSubtotal(item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(qty)));
            orderItemRepository.save(item);
        }
        recalcOrder(order);
        return toDto(order);
    }

    @Override
    public OrderResponseDTO removeOrderItem(Long orderId, Long itemId) {
        OrderEntity order = requireEditable(orderId);
        OrderItemEntity item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy món trong đơn"));
        if (item.getOrder() == null || !item.getOrder().getId().equals(orderId)) {
            throw new BusinessException("Món không thuộc đơn này");
        }
        orderItemRepository.delete(item);
        recalcOrder(order);
        return toDto(order);
    }

    /** Chỉ cho sửa khi đơn chưa hoàn tất / chưa hủy. */
    private OrderEntity requireEditable(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn món"));
        if (Set.of("COMPLETED", "CANCELLED").contains(order.getOrderStatus())) {
            throw new BusinessException("Không thể sửa món của đơn đã hoàn thành hoặc đã hủy");
        }
        return order;
    }

    private java.math.BigDecimal priceAfterDiscount(FoodEntity food) {
        java.math.BigDecimal price = food.getPrice();
        int dpct = food.getDiscountPercent() == null ? 0 : food.getDiscountPercent();
        if (dpct > 0) {
            price = price.multiply(java.math.BigDecimal.valueOf(100 - dpct))
                    .divide(java.math.BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
        }
        return price;
    }

    /** Tính lại tổng tiền đơn sau khi thêm/xóa/sửa món (giữ nguyên mức giảm voucher đã có). */
    private void recalcOrder(OrderEntity order) {
        List<OrderItemEntity> items = orderItemRepository.findByOrderId(order.getId());
        java.math.BigDecimal total = items.stream()
                .map(OrderItemEntity::getSubtotal)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal discount = order.getDiscountAmount() == null
                ? java.math.BigDecimal.ZERO : order.getDiscountAmount();
        if (discount.compareTo(total) > 0) discount = total; // không để âm
        order.setTotalAmount(total);
        order.setFinalAmount(total.subtract(discount));
        orderRepository.save(order);
    }

    private OrderResponseDTO toDto(OrderEntity order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setOrderCode(order.getOrderCode());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setDiscountAmount(order.getDiscountAmount());
        dto.setFinalAmount(order.getFinalAmount());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setPaymentStatus(order.getPaymentStatus());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setNote(order.getNote());
        dto.setCreatedAt(order.getCreatedAt());

        // ── Thông tin đặt bàn gắn với đơn món ──
        if (order.getReservation() != null) {
            ReservationEntity r = order.getReservation();
            dto.setReservationId(r.getId());
            dto.setReservationCode(r.getReservationCode());
            dto.setReservationTime(r.getReservationTime());
            dto.setPartySize(r.getPartySize());
            if (r.getTable() != null) dto.setTableNumber(r.getTable().getTableNumber());
            dto.setGuestPhone(r.getGuestPhone());
            dto.setDepositAmount(r.getDepositAmount());
            dto.setDepositStatus(r.getDepositStatus());
        }

        if (order.getUser() != null) dto.setCustomerName(order.getUser().getFullName());

        dto.setItems(orderItemRepository.findByOrderId(order.getId()).stream()
                .map(item -> {
                    OrderItemResponseDTO itemDto = new OrderItemResponseDTO();
                    itemDto.setId(item.getId());
                    itemDto.setFoodId(item.getFood().getId());
                    itemDto.setFoodName(item.getFoodName());
                    itemDto.setQuantity(item.getQuantity());
                    itemDto.setUnitPrice(item.getUnitPrice());
                    itemDto.setSubtotal(item.getSubtotal());
                    return itemDto;
                }).toList());

        return dto;
    }

    private String normalizeBlank(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toUpperCase();
    }

    private void validateOrderStatus(String status) {
        if (!Set.of("PENDING", "CONFIRMED", "PREPARING", "SERVED", "COMPLETED", "CANCELLED").contains(status)) {
            throw new BusinessException("Trạng thái đơn món không hợp lệ: " + status);
        }
    }

    private void validatePaymentStatus(String status) {
        if (!Set.of("UNPAID", "PENDING", "PAID", "FAILED", "REFUNDED").contains(status)) {
            throw new BusinessException("Trạng thái thanh toán không hợp lệ: " + status);
        }
    }
}
