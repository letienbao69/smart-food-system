package com.food.smart_food_system.Service.impl;

import com.food.smart_food_system.DTO.CreateReservationRequest;
import com.food.smart_food_system.DTO.ReservationResponseDTO;
import com.food.smart_food_system.DTO.TableDTO;
import com.food.smart_food_system.DTO.UpdateReservationStatusRequest;
import com.food.smart_food_system.Entity.*;
import com.food.smart_food_system.Exception.BusinessException;
import com.food.smart_food_system.Exception.ResourceNotFoundException;
import com.food.smart_food_system.Repository.*;
import com.food.smart_food_system.Service.CustomUserDetailsService;
import com.food.smart_food_system.Service.NotificationService;
import com.food.smart_food_system.Service.OrderService;
import com.food.smart_food_system.Service.ReservationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class ReservationServiceImpl implements ReservationService {

    /** Tiền cọc giữ bàn gợi ý trên mỗi khách (VND). Cọc được trừ vào hóa đơn khi đến ăn. */
    @Value("${reservation.deposit-fixed:20000}")
    private long depositFixed;

    /** Thời gian cho phép thanh toán cọc (phút) trước khi tự hủy. */
    @Value("${app.reservation.payment-timeout-minutes:3}")
    private int paymentTimeoutMinutes;

    private final ReservationRepository reservationRepository;
    private final RestaurantTableRepository tableRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final PaymentRepository paymentRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final VoucherRepository voucherRepository;
    private final FoodRepository foodRepository;
    private final CustomUserDetailsService customUserDetailsService;
    private final VoucherServiceImpl voucherService;
    private final NotificationService notificationService;
    private final OrderService orderService;

    public ReservationServiceImpl(
            ReservationRepository reservationRepository,
            RestaurantTableRepository tableRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            PaymentRepository paymentRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            VoucherRepository voucherRepository,
            FoodRepository foodRepository,
            CustomUserDetailsService customUserDetailsService,
            VoucherServiceImpl voucherService,
            NotificationService notificationService,
            OrderService orderService
    ) {
        this.reservationRepository = reservationRepository;
        this.tableRepository = tableRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.paymentRepository = paymentRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.voucherRepository = voucherRepository;
        this.foodRepository = foodRepository;
        this.customUserDetailsService = customUserDetailsService;
        this.voucherService = voucherService;
        this.notificationService = notificationService;
        this.orderService = orderService;
    }

    @Override
    public ReservationResponseDTO createReservation(String email, CreateReservationRequest request) {
        UserEntity user = customUserDetailsService.getUserByEmail(email);

        // Cho phép đặt bàn từ thời điểm hiện tại (có nới 30 phút để tránh lệch lúc gửi form),
        // nhưng vẫn chặn các mốc thời gian rõ ràng trong quá khứ.
        if (request.getReservationTime().isBefore(LocalDateTime.now().minusMinutes(30))) {
            throw new BusinessException("Thời gian đặt bàn phải ở hiện tại hoặc tương lai");
        }

        ReservationEntity reservation = new ReservationEntity();
        reservation.setUser(user);
        reservation.setReservationCode("RSV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        reservation.setGuestName(request.getGuestName());
        reservation.setGuestPhone(request.getGuestPhone());
        reservation.setPartySize(request.getPartySize());
        reservation.setReservationTime(request.getReservationTime());
        reservation.setStatus("PENDING");
        reservation.setNote(request.getNote());
        reservation.setPaymentMethod(normalizePaymentMethod(request.getPaymentMethod()));

        // Khách có thể chọn sẵn bàn mong muốn (nếu hợp lệ và còn dùng được)
        if (request.getTableId() != null) {
            RestaurantTableEntity table = tableRepository.findById(request.getTableId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bàn"));
            if (!"AVAILABLE".equalsIgnoreCase(table.getStatus())) {
                throw new BusinessException("Bàn đang bảo trì, vui lòng chọn bàn khác");
            }
            if (table.getCapacity() < request.getPartySize()) {
                throw new BusinessException("Bàn " + table.getTableNumber() + " chỉ chứa " + table.getCapacity() + " khách");
            }
            ensureTableFree(table.getId(), request.getReservationTime(), null);
            reservation.setTable(table);
        }

        reservationRepository.save(reservation);

        // Đặt món trước (linh hoạt): nếu khách chọn preorder -> tạo đơn món từ giỏ hàng
        boolean preorder = Boolean.TRUE.equals(request.getPreorder());
        OrderEntity preorderEntity = null;
        if (preorder) {
            preorderEntity = buildPreorderFromCart(user, reservation, request.getVoucherCode());
            reservation.setHasPreorder(true);
        }

        // Bắt buộc: thanh toán PAYOS phải CHỌN BÀN. Món đặt trước là tùy chọn:
        //  - Chỉ đặt bàn (không món)  -> cọc giữ chỗ cố định 20.000đ.
        //  - Đặt bàn kèm món          -> cọc 10% tổng tiền món.
        // Không cho thanh toán khi chỉ có món mà chưa chọn bàn.
        if ("PAYOS".equals(reservation.getPaymentMethod())) {
            if (reservation.getTable() == null) {
                throw new BusinessException("Vui lòng chọn bàn trước khi thanh toán online");
            }
        }

        // Tiền cọc giữ bàn: chỉ áp dụng khi thanh toán online qua PayOS.
        // Thanh toán online qua PayOS — số tiền cọc trả trước:
        //  - Cọc giữ bàn cố định 20.000đ.
        //  - Nếu có đặt món trước: CỘNG THÊM 10% tiền món (sau giảm giá).
        //  => Cọc online = 20.000 + 10% tiền món. Khi xuất hóa đơn sẽ hiện GIÁ ĐẦY ĐỦ
        //     của món và trừ đi phần cọc đã trả -> ra số còn phải trả tại quán.
        // Thanh toán tại nhà hàng (CASH): không cọc.
        String pm = reservation.getPaymentMethod();
        if ("PAYOS".equals(pm)) {
            BigDecimal deposit = BigDecimal.valueOf(depositFixed); // cọc giữ bàn 20.000đ
            if (preorderEntity != null) {
                BigDecimal foodDeposit = preorderEntity.getFinalAmount()
                        .multiply(BigDecimal.valueOf(10))
                        .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
                deposit = deposit.add(foodDeposit); // + 10% tiền món
            }
            reservation.setDepositAmount(deposit);
            reservation.setDepositStatus("NONE");
        } else {
            reservation.setDepositAmount(BigDecimal.ZERO);
            reservation.setDepositStatus("NONE");
        }
        reservationRepository.save(reservation);

        if (preorderEntity != null) {
            // thông báo cho admin về đơn món đặt trước
            notificationService.notifyNewOrder(preorderEntity);
        }

        notificationService.notifyNewReservation(reservation);

        return toDto(reservation);
    }

    /** Dựng đơn món đặt trước từ giỏ hàng của khách, gắn vào lượt đặt bàn. */
    private OrderEntity buildPreorderFromCart(UserEntity user, ReservationEntity reservation, String voucherCode) {
        CartEntity cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException("Giỏ hàng đang trống, không thể đặt món trước"));

        List<CartItemEntity> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new BusinessException("Giỏ hàng đang trống, không thể đặt món trước");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (CartItemEntity item : cartItems) {
            FoodEntity food = item.getFood();
            if (food.getStock() < item.getQuantity()) {
                throw new BusinessException("Món " + food.getName() + " không đủ tồn kho");
            }
            total = total.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        VoucherEntity voucher = null;
        BigDecimal discount = BigDecimal.ZERO;
        if (voucherCode != null && !voucherCode.isBlank()) {
            voucher = voucherService.requireValid(voucherCode);
            if (total.compareTo(voucher.getMinOrderValue()) < 0) {
                throw new BusinessException("Đơn chưa đạt giá trị tối thiểu để áp voucher");
            }
            if (voucher.isPercent()) {
                discount = total.multiply(voucher.getDiscountValue()).divide(BigDecimal.valueOf(100));
            } else {
                discount = voucher.getDiscountValue();
            }
            if (voucher.getMaxDiscount() != null && discount.compareTo(voucher.getMaxDiscount()) > 0) {
                discount = voucher.getMaxDiscount();
            }
            if (discount.compareTo(total) > 0) discount = total;
            voucher.setQuantity(voucher.getQuantity() - 1);
            voucherRepository.save(voucher);
        }

        OrderEntity order = new OrderEntity();
        order.setUser(user);
        order.setReservation(reservation);
        order.setVoucher(voucher);
        order.setOrderCode("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setTotalAmount(total);
        order.setDiscountAmount(discount);
        order.setFinalAmount(total.subtract(discount));
        order.setPaymentMethod(reservation.getPaymentMethod()); // thanh toán tại quán
        order.setPaymentStatus("UNPAID");
        order.setOrderStatus("PENDING");
        order.setNote("Đơn món đặt trước cho đặt bàn " + reservation.getReservationCode());
        orderRepository.save(order);

        for (CartItemEntity item : cartItems) {
            FoodEntity food = item.getFood();
            OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setOrder(order);
            orderItem.setFood(food);
            orderItem.setFoodName(food.getName());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setUnitPrice(item.getUnitPrice());
            orderItem.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            orderItemRepository.save(orderItem);

            food.setStock(food.getStock() - item.getQuantity());
            if (food.getStock() <= 0) food.setStatus("OUT_OF_STOCK");
            foodRepository.save(food);
        }

        OrderStatusHistoryEntity history = new OrderStatusHistoryEntity();
        history.setOrder(order);
        history.setStatus(order.getOrderStatus());
        history.setNote("Khởi tạo đơn món đặt trước");
        orderStatusHistoryRepository.save(history);

        PaymentEntity payment = new PaymentEntity();
        payment.setOrder(order);
        payment.setProvider(order.getPaymentMethod());
        payment.setAmount(order.getFinalAmount());
        payment.setStatus("PENDING");
        paymentRepository.save(payment);

        cartItemRepository.deleteByCartId(cart.getId());
        return order;
    }

    @Override
    public List<ReservationResponseDTO> getMyReservations(String email) {
        UserEntity user = customUserDetailsService.getUserByEmail(email);
        return reservationRepository.findByUserIdOrderByIdDesc(user.getId())
                .stream().map(this::toDto).toList();
    }

    @Override
    public ReservationResponseDTO getMyReservationDetail(String email, Long id) {
        UserEntity user = customUserDetailsService.getUserByEmail(email);
        ReservationEntity r = reservationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt đặt bàn"));
        return toDto(r);
    }

    @Override
    public ReservationResponseDTO cancelMyReservation(String email, Long id) {
        UserEntity user = customUserDetailsService.getUserByEmail(email);
        ReservationEntity r = reservationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt đặt bàn"));
        if (!Set.of("PENDING", "CONFIRMED").contains(r.getStatus())) {
            throw new BusinessException("Chỉ có thể hủy khi đặt bàn đang Chờ xác nhận hoặc Đã xác nhận");
        }
        applyCancellation(r, "Đặt bàn đã bị hủy");
        // Báo cho admin/staff (lưu thông báo + đẩy real-time) và cho khách
        notificationService.notifyReservationCancelledByCustomer(r);
        return toDto(r);
    }

    /**
     * Hủy một lượt đặt bàn: đổi trạng thái CANCELLED, hủy đơn món đặt trước (nếu có),
     * giải phóng bàn. KHÔNG gửi thông báo cấp đặt bàn — bên gọi tự chọn thông báo phù hợp.
     */
    private void applyCancellation(ReservationEntity r, String orderCancelNote) {
        r.setStatus("CANCELLED");
        r.setDepositRequestedAt(null);
        reservationRepository.save(r);

        orderRepository.findByReservationId(r.getId()).forEach(o -> {
            if (!"CANCELLED".equalsIgnoreCase(o.getOrderStatus())) {
                String oldOrderStatus = o.getOrderStatus();
                o.setOrderStatus("CANCELLED");
                orderRepository.save(o);
                notificationService.notifyOrderStatusChanged(o, oldOrderStatus, orderCancelNote);
            }
        });

        if (r.getTable() != null && !"AVAILABLE".equalsIgnoreCase(r.getTable().getStatus())) {
            r.getTable().setStatus("AVAILABLE");
            tableRepository.save(r.getTable());
        }
    }

    /**
     * Xác nhận đã nhận cọc qua cổng thanh toán (PayOS) — gọi tự động từ webhook.
     * Tự động chuyển đặt bàn sang ĐÃ XÁC NHẬN mà không cần admin/staff bấm nút,
     * đồng thời giữ bàn và đồng bộ đơn món đặt trước. Idempotent.
     */
    @Override
    public ReservationResponseDTO markDepositPaidByGateway(Long id) {
        ReservationEntity r = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt đặt bàn"));

        if ("PAID".equalsIgnoreCase(r.getDepositStatus())) {
            return toDto(r); // đã xử lý rồi
        }

        r.setDepositStatus("PAID");
        r.setDepositRequestedAt(null); // đã trả -> không còn bị tính quá hạn

        // Trường hợp thanh toán trễ cho đơn đã hủy: ghi nhận đã trả, không khôi phục
        if ("CANCELLED".equalsIgnoreCase(r.getStatus())) {
            reservationRepository.save(r);
            notificationService.notifyDepositPaid(r);
            return toDto(r);
        }

        String oldStatus = r.getStatus();
        boolean autoConfirmed = false;
        if ("PENDING".equalsIgnoreCase(r.getStatus())) {
            r.setStatus("CONFIRMED");
            autoConfirmed = true;
            if (r.getTable() != null) {
                r.getTable().setStatus("RESERVED");
                tableRepository.save(r.getTable());
            }
            orderRepository.findByReservationId(r.getId()).forEach(o -> {
                if (!"CANCELLED".equalsIgnoreCase(o.getOrderStatus())) {
                    o.setOrderStatus("CONFIRMED");
                    orderRepository.save(o);
                }
            });
        }
        reservationRepository.save(r);

        notificationService.notifyDepositPaid(r);
        if (autoConfirmed) {
            notificationService.notifyReservationStatusChanged(r, oldStatus, "Tự động xác nhận sau khi nhận cọc");
        }
        return toDto(r);
    }

    /**
     * Tự động hủy các lượt đặt bàn đã tạo link cọc nhưng quá thời gian quy định
     * mà khách chưa thanh toán. Trả về số lượt vừa hủy.
     */
    @Override
    public int autoCancelExpiredDepositReservations(int timeoutMinutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<ReservationEntity> expired = reservationRepository
                .findByStatusAndDepositStatusNotAndDepositRequestedAtBefore("PENDING", "PAID", cutoff);

        int count = 0;
        for (ReservationEntity r : expired) {
            if (r.getDepositRequestedAt() == null) continue; // an toàn
            applyCancellation(r, "Đặt bàn tự hủy do quá hạn thanh toán cọc");
            notificationService.notifyReservationAutoCancelled(r);
            count++;
        }
        return count;
    }

    @Override
    public ReservationResponseDTO notifyDepositPaid(String email, Long id) {
        UserEntity user = customUserDetailsService.getUserByEmail(email);
        ReservationEntity r = reservationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt đặt bàn"));
        if ("PAID".equalsIgnoreCase(r.getDepositStatus())) {
            throw new BusinessException("Tiền cọc đã được xác nhận rồi");
        }
        if ("CANCELLED".equalsIgnoreCase(r.getStatus())) {
            throw new BusinessException("Lượt đặt bàn đã bị hủy");
        }
        r.setDepositStatus("PENDING");
        reservationRepository.save(r);
        notificationService.notifyDepositClaimed(r);
        return toDto(r);
    }

    @Override
    public List<ReservationResponseDTO> getAll() {
        return reservationRepository.findAllByOrderByReservationTimeDesc()
                .stream().map(this::toDto).toList();
    }

    @Override
    public ReservationResponseDTO getDetailForAdmin(Long id) {
        return toDto(require(id));
    }

    @Override
    public ReservationResponseDTO updateStatus(Long id, UpdateReservationStatusRequest request) {
        ReservationEntity r = require(id);
        String oldStatus = r.getStatus();

        // Gán/đổi bàn
        if (request.getTableId() != null) {
            RestaurantTableEntity table = tableRepository.findById(request.getTableId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bàn"));
            if (table.getCapacity() < r.getPartySize()) {
                throw new BusinessException("Bàn " + table.getTableNumber() + " chỉ chứa " + table.getCapacity() + " khách");
            }
            ensureTableFree(table.getId(), r.getReservationTime(), r.getId());
            r.setTable(table);
        }

        if (request.getDepositStatus() != null && !request.getDepositStatus().isBlank()) {
            String d = request.getDepositStatus().trim().toUpperCase();
            if (!Set.of("NONE", "PENDING", "PAID").contains(d)) {
                throw new BusinessException("Trạng thái cọc không hợp lệ: " + d);
            }
            r.setDepositStatus(d);
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            String s = request.getStatus().trim().toUpperCase();
            validateStatus(s);
            validateTransition(r.getStatus(), s);
            r.setStatus(s);
            // Khách không đến (NO_SHOW): nếu đã đặt cọc thì cọc bị mất (đánh dấu đã thu)
            if ("NO_SHOW".equals(s) && "PAID".equalsIgnoreCase(r.getDepositStatus())) {
                r.setNote((r.getNote() == null ? "" : r.getNote() + " | ") + "Khách không đến, cọc không hoàn lại.");
            }
            // Tự động cập nhật trạng thái bàn theo vòng đời đặt bàn
            RestaurantTableEntity table = r.getTable();
            if (table != null) {
                if ("CONFIRMED".equals(s)) {
                    table.setStatus("RESERVED");   // đã xác nhận -> giữ bàn, ẩn khỏi danh sách chọn
                    tableRepository.save(table);
                } else if ("SEATED".equals(s)) {
                    table.setStatus("OCCUPIED");
                    tableRepository.save(table);
                } else if (Set.of("COMPLETED", "CANCELLED", "NO_SHOW").contains(s)) {
                    table.setStatus("AVAILABLE");
                    tableRepository.save(table);
                }
            }
            // Đồng bộ trạng thái đơn món đặt trước (nếu có) theo vòng đời đặt bàn
            String orderStatus = switch (s) {
                case "CONFIRMED" -> "CONFIRMED";
                case "SEATED" -> "SERVED";
                case "COMPLETED" -> "COMPLETED";
                case "CANCELLED", "NO_SHOW" -> "CANCELLED";
                default -> null;
            };
            if (orderStatus != null) {
                orderRepository.findByReservationId(r.getId()).forEach(o -> {
                    o.setOrderStatus(orderStatus);
                    if ("COMPLETED".equals(orderStatus)) o.setPaymentStatus("PAID");
                    orderRepository.save(o);
                });
            }
        }

        if (request.getNote() != null) r.setNote(request.getNote());

        reservationRepository.save(r);

        if (request.getStatus() != null && !request.getStatus().equalsIgnoreCase(oldStatus)) {
            notificationService.notifyReservationStatusChanged(r, oldStatus, request.getNote());
        }
        return toDto(r);
    }

    @Override
    public void deleteReservation(Long id) {
        ReservationEntity r = require(id);
        if (!Set.of("CANCELLED", "COMPLETED", "NO_SHOW").contains(r.getStatus())) {
            throw new BusinessException("Chỉ xóa được lượt đặt bàn đã Hủy / Hoàn tất / Không đến");
        }
        // Gỡ liên kết đơn món đặt trước (giữ lịch sử đơn món)
        orderRepository.findByReservationId(id)
                .forEach(o -> { o.setReservation(null); orderRepository.save(o); });
        reservationRepository.delete(r);
    }

    // ── Helpers ──
    private ReservationEntity require(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt đặt bàn"));
    }

    /** Chặn 2 lượt đặt cùng 1 bàn trong khung ±90 phút (trừ chính nó). */
    private void ensureTableFree(Long tableId, LocalDateTime time, Long selfId) {
        LocalDateTime from = time.minusMinutes(90);
        LocalDateTime to = time.plusMinutes(90);
        boolean clash = reservationRepository
                .findByTableIdAndStatusInAndReservationTimeBetween(
                        tableId, List.of("PENDING", "CONFIRMED", "SEATED"), from, to)
                .stream().anyMatch(r -> selfId == null || !r.getId().equals(selfId));
        if (clash) {
            throw new BusinessException("Bàn đã có lượt đặt khác trong khung giờ này");
        }
    }

    private String normalizePaymentMethod(String method) {
        if (method == null || method.isBlank()) return "PAYOS";
        String m = method.trim().toUpperCase();
        if (!Set.of("PAYOS", "CASH").contains(m)) {
            throw new BusinessException("Hình thức thanh toán không hợp lệ: " + method);
        }
        return m;
    }

    private void validateStatus(String s) {
        if (!Set.of("PENDING", "CONFIRMED", "SEATED", "COMPLETED", "CANCELLED", "NO_SHOW").contains(s)) {
            throw new BusinessException("Trạng thái đặt bàn không hợp lệ: " + s);
        }
    }

    // Kiểm tra chuyển trạng thái hợp lệ theo vòng đời đặt bàn (không cho lùi từ trạng thái kết thúc)
    private void validateTransition(String from, String to) {
        if (from == null || from.equalsIgnoreCase(to)) return;
        from = from.toUpperCase();
        // Các trạng thái kết thúc: không được chuyển sang trạng thái khác
        if (Set.of("COMPLETED", "CANCELLED", "NO_SHOW").contains(from)) {
            throw new BusinessException("Lượt đặt bàn đã ở trạng thái kết thúc (" + from + "), không thể thay đổi.");
        }
        java.util.Map<String, Set<String>> allowed = java.util.Map.of(
                "PENDING",   Set.of("CONFIRMED", "CANCELLED", "NO_SHOW"),
                "CONFIRMED", Set.of("SEATED", "CANCELLED", "NO_SHOW"),
                "SEATED",    Set.of("COMPLETED", "CANCELLED")
        );
        Set<String> next = allowed.get(from);
        if (next != null && !next.contains(to)) {
            throw new BusinessException("Không thể chuyển từ " + from + " sang " + to + ".");
        }
    }

    private ReservationResponseDTO toDto(ReservationEntity r) {
        ReservationResponseDTO dto = new ReservationResponseDTO();
        dto.setId(r.getId());
        dto.setReservationCode(r.getReservationCode());
        dto.setGuestName(r.getGuestName());
        dto.setGuestPhone(r.getGuestPhone());
        dto.setPartySize(r.getPartySize());
        dto.setReservationTime(r.getReservationTime());
        dto.setStatus(r.getStatus());
        dto.setDepositAmount(r.getDepositAmount());
        dto.setDepositStatus(r.getDepositStatus());
        dto.setPaymentMethod(r.getPaymentMethod());
        dto.setHasPreorder(r.getHasPreorder());
        dto.setNote(r.getNote());
        dto.setCreatedAt(r.getCreatedAt());
        // Mốc tự hủy nếu chưa thanh toán cọc đúng hạn (chỉ khi đang chờ thanh toán PayOS)
        if (r.getDepositRequestedAt() != null
                && "PENDING".equalsIgnoreCase(r.getStatus())
                && !"PAID".equalsIgnoreCase(r.getDepositStatus())) {
            dto.setDepositExpiresAt(r.getDepositRequestedAt().plusMinutes(paymentTimeoutMinutes));
        }
        if (r.getUser() != null) dto.setCustomerName(r.getUser().getFullName());

        if (r.getTable() != null) {
            TableDTO t = new TableDTO();
            t.setId(r.getTable().getId());
            t.setTableNumber(r.getTable().getTableNumber());
            t.setCapacity(r.getTable().getCapacity());
            t.setZone(r.getTable().getZone());
            t.setStatus(r.getTable().getStatus());
            dto.setTable(t);
        }

        // Đơn món gắn với lượt đặt bàn (đặt trước HOẶC nhân viên gọi thêm tại bàn)
        orderRepository.findByReservationId(r.getId()).stream()
                .findFirst()
                .ifPresent(o -> dto.setPreorder(orderService.getOrderDetailForAdmin(o.getId())));
        return dto;
    }
}
