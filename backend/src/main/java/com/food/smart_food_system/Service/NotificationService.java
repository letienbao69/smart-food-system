package com.food.smart_food_system.Service;

import com.food.smart_food_system.DTO.NotificationResponseDTO;
import com.food.smart_food_system.Entity.NotificationEntity;
import com.food.smart_food_system.Entity.OrderEntity;
import com.food.smart_food_system.Entity.ReservationEntity;
import com.food.smart_food_system.Entity.UserEntity;
import com.food.smart_food_system.Exception.ResourceNotFoundException;
import com.food.smart_food_system.Repository.NotificationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional
public class NotificationService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_CUSTOMER = "CUSTOMER";

    private final NotificationRepository notificationRepository;
    private final CustomUserDetailsService customUserDetailsService;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(
            NotificationRepository notificationRepository,
            CustomUserDetailsService customUserDetailsService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.notificationRepository = notificationRepository;
        this.customUserDetailsService = customUserDetailsService;
        this.messagingTemplate = messagingTemplate;
    }

    public void notifyNewOrder(OrderEntity order) {
        notifyAdminNewOrder(order);
        notifyUserOrderCreated(order);
    }

    private void notifyAdminNewOrder(OrderEntity order) {
        String customerName = order.getUser() != null ? order.getUser().getFullName() : "Khách hàng";
        String amountText = formatMoney(order.getFinalAmount());

        NotificationEntity notification = new NotificationEntity();
        notification.setType("ORDER_CREATED");
        notification.setTitle("Có đơn hàng mới");
        notification.setMessage(customerName + " vừa đặt đơn " + order.getOrderCode() + " với tổng tiền " + amountText);
        notification.setTargetRole(ROLE_ADMIN);
        notification.setTargetUser(null);
        notification.setReferenceType("ORDER");
        notification.setReferenceId(order.getId());
        notification.setReadStatus(false);

        notificationRepository.save(notification);

        // ── WebSocket real-time push to all admin sessions ──
        try {
            messagingTemplate.convertAndSend("/topic/admin/orders", Map.of(
                    "type", "NEW_ORDER",
                    "title", "Có đơn hàng mới",
                    "message", customerName + " vừa đặt đơn " + order.getOrderCode() + " — " + amountText,
                    "orderCode", order.getOrderCode(),
                    "orderId", order.getId()
            ));
        } catch (Exception ignored) {
            // WebSocket push failure must never break the HTTP order-creation flow
        }
    }

    private void notifyUserOrderCreated(OrderEntity order) {
        if (order.getUser() == null) {
            return;
        }

        NotificationEntity notification = new NotificationEntity();
        notification.setType("ORDER_CREATED");
        notification.setTitle("Đặt hàng thành công");
        notification.setMessage("Đơn hàng " + order.getOrderCode() + " của bạn đã được tạo thành công. Trạng thái hiện tại: Chờ xác nhận.");
        notification.setTargetRole(ROLE_CUSTOMER);
        notification.setTargetUser(order.getUser());
        notification.setReferenceType("ORDER");
        notification.setReferenceId(order.getId());
        notification.setReadStatus(false);

        notificationRepository.save(notification);

        // ── WebSocket push to the specific customer ──
        try {
            String email = order.getUser().getEmail();
            messagingTemplate.convertAndSendToUser(email, "/queue/orders", Map.of(
                    "type", "ORDER_CREATED",
                    "title", "Đặt hàng thành công",
                    "message", "Đơn hàng " + order.getOrderCode() + " đã tạo thành công. Chờ xác nhận.",
                    "orderCode", order.getOrderCode(),
                    "orderId", order.getId()
            ));
        } catch (Exception ignored) {}
    }

    // Thông báo cho khách khi yêu cầu liên hệ/phản ánh được xử lý xong
    public void notifyContactResolved(UserEntity user, String subject, String reply) {
        if (user == null) return;
        String msg = (reply != null && !reply.isBlank())
                ? reply
                : "Yêu cầu liên hệ" + (subject != null && !subject.isBlank() ? " \"" + subject + "\"" : "") + " của bạn đã được xử lý. Cảm ơn bạn đã phản hồi!";

        NotificationEntity notification = new NotificationEntity();
        notification.setType("CONTACT_RESOLVED");
        notification.setTitle("Phản hồi liên hệ của bạn");
        notification.setMessage(msg);
        notification.setTargetRole(ROLE_CUSTOMER);
        notification.setTargetUser(user);
        notification.setReferenceType("CONTACT");
        notification.setReadStatus(false);
        notificationRepository.save(notification);

        try {
            messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/orders", Map.of(
                    "type", "CONTACT_RESOLVED",
                    "title", "Phản hồi liên hệ của bạn",
                    "message", msg
            ));
        } catch (Exception ignored) {}
    }

    public void notifyOrderStatusChanged(OrderEntity order, String oldStatus, String note) {
        if (order.getUser() == null) {
            return;
        }

        String newStatus = order.getOrderStatus();

        NotificationEntity notification = new NotificationEntity();
        notification.setType("ORDER_STATUS_UPDATED");
        notification.setTitle("Cập nhật trạng thái đơn hàng");
        notification.setMessage(
                "Đơn hàng " + order.getOrderCode()
                        + " đã chuyển từ \"" + getOrderStatusText(oldStatus)
                        + "\" sang \"" + getOrderStatusText(newStatus) + "\""
                        + (note != null && !note.isBlank() ? ". Ghi chú: " + note : "")
        );
        notification.setTargetRole(ROLE_CUSTOMER);
        notification.setTargetUser(order.getUser());
        notification.setReferenceType("ORDER");
        notification.setReferenceId(order.getId());
        notification.setReadStatus(false);

        notificationRepository.save(notification);

        // ── WebSocket real-time push to customer ──
        try {
            messagingTemplate.convertAndSendToUser(order.getUser().getEmail(), "/queue/orders", Map.of(
                    "type", "ORDER_STATUS_CHANGED",
                    "title", "Đơn hàng cập nhật",
                    "message", "Đơn hàng " + order.getOrderCode() + " → " + getOrderStatusText(newStatus),
                    "orderCode", order.getOrderCode(),
                    "orderId", order.getId(),
                    "newStatus", newStatus
            ));
        } catch (Exception ignored) {}
    }

    public void notifyPaymentStatusChanged(OrderEntity order, String oldPaymentStatus) {
        String newPaymentStatus = order.getPaymentStatus();
        String customerName = order.getUser() != null ? order.getUser().getFullName() : "Khách hàng";

        // ── Lưu thông báo cho khách hàng ──
        if (order.getUser() != null) {
            NotificationEntity custNotif = new NotificationEntity();
            custNotif.setType("PAYMENT_UPDATED");
            custNotif.setTitle("Cập nhật thanh toán");
            custNotif.setMessage(
                    "Thanh toán của đơn hàng " + order.getOrderCode()
                            + " đã chuyển từ \"" + getPaymentStatusText(oldPaymentStatus)
                            + "\" sang \"" + getPaymentStatusText(newPaymentStatus) + "\"."
            );
            custNotif.setTargetRole(ROLE_CUSTOMER);
            custNotif.setTargetUser(order.getUser());
            custNotif.setReferenceType("ORDER");
            custNotif.setReferenceId(order.getId());
            custNotif.setReadStatus(false);
            notificationRepository.save(custNotif);

            // WebSocket push to customer
            try {
                messagingTemplate.convertAndSendToUser(order.getUser().getEmail(), "/queue/orders", Map.of(
                        "type", "PAYMENT_UPDATED",
                        "title", "Cập nhật thanh toán",
                        "message", "Đơn " + order.getOrderCode() + " — " + getPaymentStatusText(newPaymentStatus),
                        "orderCode", order.getOrderCode(),
                        "orderId", order.getId(),
                        "newPaymentStatus", newPaymentStatus
                ));
            } catch (Exception ignored) {}
        }

        // ── Lưu thông báo cho Admin và push WebSocket real-time ──
        // Quan trọng khi khách thanh toán tại quán → admin biết ngay
        NotificationEntity adminNotif = new NotificationEntity();
        adminNotif.setType("PAYMENT_UPDATED");
        adminNotif.setTitle("Xác nhận thanh toán");
        adminNotif.setMessage(
                customerName + " vừa xác nhận thanh toán đơn " + order.getOrderCode()
                        + " (" + order.getPaymentMethod() + ") — " + getPaymentStatusText(newPaymentStatus)
        );
        adminNotif.setTargetRole(ROLE_ADMIN);
        adminNotif.setTargetUser(null);
        adminNotif.setReferenceType("ORDER");
        adminNotif.setReferenceId(order.getId());
        adminNotif.setReadStatus(false);
        notificationRepository.save(adminNotif);

        try {
            messagingTemplate.convertAndSend("/topic/admin/orders", Map.of(
                    "type", "PAYMENT_CONFIRMED",
                    "title", "Thanh toán được xác nhận",
                    "message", customerName + " xác nhận thanh toán đơn " + order.getOrderCode()
                            + " — " + getPaymentStatusText(newPaymentStatus),
                    "orderCode", order.getOrderCode(),
                    "orderId", order.getId(),
                    "newPaymentStatus", newPaymentStatus
            ));
        } catch (Exception ignored) {}
    }

    public List<NotificationResponseDTO> getAdminNotifications() {
        return notificationRepository.findByTargetRoleOrderByCreatedAtDesc(ROLE_ADMIN)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<NotificationResponseDTO> getUnreadAdminNotifications() {
        return notificationRepository.findByTargetRoleAndReadStatusOrderByCreatedAtDesc(ROLE_ADMIN, false)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public Long countUnreadAdminNotifications() {
        return notificationRepository.countByTargetRoleAndReadStatus(ROLE_ADMIN, false);
    }

    public NotificationResponseDTO markAsRead(Long id) {
        NotificationEntity notification = notificationRepository.findByIdAndTargetRole(id, ROLE_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo admin"));

        notification.setReadStatus(true);
        return toDto(notificationRepository.save(notification));
    }

    public void markAllAdminAsRead() {
        List<NotificationEntity> notifications =
                notificationRepository.findByTargetRoleAndReadStatusOrderByCreatedAtDesc(ROLE_ADMIN, false);

        for (NotificationEntity notification : notifications) {
            notification.setReadStatus(true);
        }

        notificationRepository.saveAll(notifications);
    }

    public void deleteAdminNotification(Long id) {
        NotificationEntity notification = notificationRepository.findByIdAndTargetRole(id, ROLE_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo admin"));

        notificationRepository.delete(notification);
    }

    public void deleteAllAdminNotifications() {
        List<NotificationEntity> notifications =
                notificationRepository.findByTargetRoleOrderByCreatedAtDesc(ROLE_ADMIN);

        notificationRepository.deleteAll(notifications);
    }

    public List<NotificationResponseDTO> getMyNotifications(String email) {
        UserEntity user = customUserDetailsService.getUserByEmail(email);

        return notificationRepository.findByTargetUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<NotificationResponseDTO> getUnreadMyNotifications(String email) {
        UserEntity user = customUserDetailsService.getUserByEmail(email);

        return notificationRepository.findByTargetUserIdAndReadStatusOrderByCreatedAtDesc(user.getId(), false)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public Long countUnreadMyNotifications(String email) {
        UserEntity user = customUserDetailsService.getUserByEmail(email);

        return notificationRepository.countByTargetUserIdAndReadStatus(user.getId(), false);
    }

    public NotificationResponseDTO markMyNotificationAsRead(String email, Long id) {
        UserEntity user = customUserDetailsService.getUserByEmail(email);

        NotificationEntity notification = notificationRepository.findByIdAndTargetUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo của người dùng"));

        notification.setReadStatus(true);

        return toDto(notificationRepository.save(notification));
    }

    public void markAllMyNotificationsAsRead(String email) {
        UserEntity user = customUserDetailsService.getUserByEmail(email);

        List<NotificationEntity> notifications =
                notificationRepository.findByTargetUserIdAndReadStatus(user.getId(), false);

        for (NotificationEntity notification : notifications) {
            notification.setReadStatus(true);
        }

        notificationRepository.saveAll(notifications);
    }

    // ── Xóa thông báo ──────────────────────────────────────────

    public void deleteMyNotification(String email, Long id) {
        UserEntity user = customUserDetailsService.getUserByEmail(email);

        NotificationEntity notification = notificationRepository.findByIdAndTargetUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo"));

        notificationRepository.delete(notification);
    }

    public void deleteAllMyNotifications(String email) {
        UserEntity user = customUserDetailsService.getUserByEmail(email);

        List<NotificationEntity> notifications =
                notificationRepository.findByTargetUserIdOrderByCreatedAtDesc(user.getId());

        notificationRepository.deleteAll(notifications);
    }

    private NotificationResponseDTO toDto(NotificationEntity notification) {
        NotificationResponseDTO dto = new NotificationResponseDTO();

        dto.setId(notification.getId());
        dto.setType(notification.getType());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setTargetRole(notification.getTargetRole());
        dto.setReferenceType(notification.getReferenceType());
        dto.setReferenceId(notification.getReferenceId());
        dto.setReadStatus(notification.getReadStatus());
        dto.setCreatedAt(notification.getCreatedAt());

        if (notification.getTargetUser() != null) {
            dto.setTargetUserId(notification.getTargetUser().getId());
            dto.setTargetUserName(notification.getTargetUser().getFullName());
        }

        return dto;
    }

    private String getOrderStatusText(String status) {
        if (status == null || status.isBlank()) {
            return "Chưa xác định";
        }

        return switch (status.toUpperCase()) {
            case "PENDING" -> "Chờ xác nhận";
            case "CONFIRMED" -> "Đã xác nhận";
            case "PREPARING" -> "Đang chuẩn bị món";
            case "SERVED" -> "Đã phục vụ";
            case "COMPLETED" -> "Hoàn thành";
            case "CANCELLED" -> "Đã hủy";
            default -> status;
        };
    }

    private String getPaymentStatusText(String status) {
        if (status == null || status.isBlank()) {
            return "Chưa xác định";
        }

        return switch (status.toUpperCase()) {
            case "UNPAID" -> "Chưa thanh toán";
            case "PENDING" -> "Đang chờ thanh toán";
            case "PAID" -> "Đã thanh toán";
            case "FAILED" -> "Thanh toán thất bại";
            case "REFUNDED" -> "Đã hoàn tiền";
            default -> status;
        };
    }

    /**
     * Khách hàng báo đã chuyển khoản — thông báo admin xác nhận thủ công.
     * Không tự đổi paymentStatus.
     */
    public void notifyOnlinePaymentClaimed(OrderEntity order) {
        String customerName = order.getUser() != null ? order.getUser().getFullName() : "Khách hàng";
        String amountText = formatMoney(order.getFinalAmount());
        String method = order.getPaymentMethod();

        // Lưu DB
        NotificationEntity notification = new NotificationEntity();
        notification.setType("PAYMENT_CLAIMED");
        notification.setTitle("Xác nhận chuyển khoản " + method);
        notification.setMessage(
                customerName + " xác nhận đã chuyển khoản " + amountText
                        + " qua " + method + " cho đơn " + order.getOrderCode()
                        + ". Vui lòng kiểm tra tài khoản và xác nhận."
        );
        notification.setTargetRole(ROLE_ADMIN);
        notification.setTargetUser(null);
        notification.setReferenceType("ORDER");
        notification.setReferenceId(order.getId());
        notification.setReadStatus(false);
        notificationRepository.save(notification);

        // WebSocket push ngay lập tức đến admin
        try {
            messagingTemplate.convertAndSend("/topic/admin/orders", Map.of(
                    "type", "PAYMENT_CLAIMED",
                    "title", "Xác nhận chuyển khoản " + method,
                    "message", customerName + " nói đã chuyển " + amountText + " qua " + method
                            + " — đơn " + order.getOrderCode() + ". Kiểm tra và xác nhận!",
                    "orderCode", order.getOrderCode(),
                    "orderId", order.getId()
            ));
        } catch (Exception ignored) {}

        // Thông báo phản hồi lại cho khách hàng
        if (order.getUser() != null) {
            try {
                messagingTemplate.convertAndSendToUser(order.getUser().getEmail(), "/queue/orders", Map.of(
                        "type", "PAYMENT_CLAIMED",
                        "title", "Đã gửi xác nhận thanh toán",
                        "message", "Chúng tôi đã nhận được thông báo. Admin đang kiểm tra đơn "
                                + order.getOrderCode() + ". Vui lòng chờ xác nhận.",
                        "orderCode", order.getOrderCode(),
                        "orderId", order.getId()
                ));
            } catch (Exception ignored) {}
        }
    }

    // ════════════════════════════════════════════════════════════
    //  ĐẶT BÀN (mô hình ăn tại nhà hàng)
    // ════════════════════════════════════════════════════════════

    /** Khách vừa đặt bàn -> báo admin + xác nhận lại cho khách. */
    public void notifyNewReservation(ReservationEntity r) {
        String customerName = r.getUser() != null ? r.getUser().getFullName() : r.getGuestName();

        NotificationEntity adminNotif = new NotificationEntity();
        adminNotif.setType("RESERVATION_CREATED");
        adminNotif.setTitle("Có lượt đặt bàn mới");
        adminNotif.setMessage(customerName + " đặt bàn " + r.getReservationCode()
                + " cho " + r.getPartySize() + " khách lúc " + formatTime(r.getReservationTime()));
        adminNotif.setTargetRole(ROLE_ADMIN);
        adminNotif.setTargetUser(null);
        adminNotif.setReferenceType("RESERVATION");
        adminNotif.setReferenceId(r.getId());
        adminNotif.setReadStatus(false);
        notificationRepository.save(adminNotif);

        try {
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("type", "NEW_RESERVATION");
            payload.put("title", "Đơn hàng mới!");
            payload.put("message", "Vừa nhận được đơn đặt bàn");
            payload.put("reservationId", r.getId());
            payload.put("reservationCode", r.getReservationCode());
            payload.put("guestName", customerName);
            payload.put("guestPhone", r.getGuestPhone());
            payload.put("partySize", r.getPartySize());
            payload.put("reservationTime", r.getReservationTime() == null ? "" : r.getReservationTime().toString());
            payload.put("tableNumber", r.getTable() != null ? r.getTable().getTableNumber() : "");
            payload.put("tableZone", r.getTable() != null && r.getTable().getZone() != null ? r.getTable().getZone() : "");
            payload.put("paymentMethod", r.getPaymentMethod() == null ? "" : r.getPaymentMethod());
            payload.put("hasPreorder", Boolean.TRUE.equals(r.getHasPreorder()));
            messagingTemplate.convertAndSend("/topic/admin/orders", payload);
        } catch (Exception ignored) {}

        if (r.getUser() != null) {
            NotificationEntity custNotif = new NotificationEntity();
            custNotif.setType("RESERVATION_CREATED");
            custNotif.setTitle("Đặt bàn thành công");
            custNotif.setMessage("Lượt đặt bàn " + r.getReservationCode()
                    + " đã được tạo. Trạng thái: Chờ xác nhận.");
            custNotif.setTargetRole(ROLE_CUSTOMER);
            custNotif.setTargetUser(r.getUser());
            custNotif.setReferenceType("RESERVATION");
            custNotif.setReferenceId(r.getId());
            custNotif.setReadStatus(false);
            notificationRepository.save(custNotif);

            try {
                messagingTemplate.convertAndSendToUser(r.getUser().getEmail(), "/queue/orders", Map.of(
                        "type", "RESERVATION_CREATED",
                        "title", "Đặt bàn thành công",
                        "message", "Lượt đặt bàn " + r.getReservationCode() + " đã tạo. Chờ xác nhận.",
                        "orderCode", r.getReservationCode(),
                        "orderId", r.getId()
                ));
            } catch (Exception ignored) {}
        }
    }

    /** Admin đổi trạng thái đặt bàn -> báo khách. */
    public void notifyReservationStatusChanged(ReservationEntity r, String oldStatus, String note) {
        String newStatus = r.getStatus();
        String customerName = r.getUser() != null ? r.getUser().getFullName() : r.getGuestName();

        // ── Real-time push cho ADMIN/STAFF (mọi lượt đặt, kể cả khách vãng lai) ──
        try {
            messagingTemplate.convertAndSend("/topic/admin/orders", Map.of(
                    "type", "RESERVATION_STATUS_CHANGED",
                    "title", "Đặt bàn cập nhật",
                    "message", (customerName == null ? "Khách" : customerName)
                            + " · đặt bàn " + r.getReservationCode()
                            + " → " + getReservationStatusText(newStatus),
                    "reservationId", r.getId(),
                    "orderCode", r.getReservationCode(),
                    "newStatus", newStatus
            ));
        } catch (Exception ignored) {}

        if (r.getUser() == null) return;

        NotificationEntity notification = new NotificationEntity();
        notification.setType("RESERVATION_STATUS_UPDATED");
        notification.setTitle("Cập nhật lượt đặt bàn");
        notification.setMessage("Đặt bàn " + r.getReservationCode()
                + " đã chuyển từ \"" + getReservationStatusText(oldStatus)
                + "\" sang \"" + getReservationStatusText(newStatus) + "\""
                + (r.getTable() != null ? ". Bàn: " + r.getTable().getTableNumber() : "")
                + (note != null && !note.isBlank() ? ". Ghi chú: " + note : ""));
        notification.setTargetRole(ROLE_CUSTOMER);
        notification.setTargetUser(r.getUser());
        notification.setReferenceType("RESERVATION");
        notification.setReferenceId(r.getId());
        notification.setReadStatus(false);
        notificationRepository.save(notification);

        try {
            messagingTemplate.convertAndSendToUser(r.getUser().getEmail(), "/queue/orders", Map.of(
                    "type", "RESERVATION_STATUS_CHANGED",
                    "title", "Đặt bàn cập nhật",
                    "message", "Đặt bàn " + r.getReservationCode() + " → " + getReservationStatusText(newStatus),
                    "orderCode", r.getReservationCode(),
                    "orderId", r.getId(),
                    "newStatus", newStatus
            ));
        } catch (Exception ignored) {}
    }

    /** Khách báo đã chuyển khoản tiền cọc qua QR -> admin xác nhận thủ công. */
    public void notifyDepositClaimed(ReservationEntity r) {
        String customerName = r.getUser() != null ? r.getUser().getFullName() : r.getGuestName();
        String amountText = formatMoney(r.getDepositAmount());

        NotificationEntity notification = new NotificationEntity();
        notification.setType("DEPOSIT_CLAIMED");
        notification.setTitle("Xác nhận tiền cọc giữ bàn");
        notification.setMessage(customerName + " báo đã chuyển khoản cọc " + amountText
                + " cho đặt bàn " + r.getReservationCode() + ". Vui lòng kiểm tra và xác nhận.");
        notification.setTargetRole(ROLE_ADMIN);
        notification.setTargetUser(null);
        notification.setReferenceType("RESERVATION");
        notification.setReferenceId(r.getId());
        notification.setReadStatus(false);
        notificationRepository.save(notification);

        try {
            messagingTemplate.convertAndSend("/topic/admin/orders", Map.of(
                    "type", "DEPOSIT_CLAIMED",
                    "title", "Xác nhận tiền cọc giữ bàn",
                    "message", customerName + " chuyển cọc " + amountText
                            + " — đặt bàn " + r.getReservationCode() + ". Kiểm tra và xác nhận!",
                    "orderCode", r.getReservationCode(),
                    "orderId", r.getId()
            ));
        } catch (Exception ignored) {}
    }

    /**
     * Thanh toán cọc PayOS thành công (tự động qua webhook).
     * Khác với notifyDepositClaimed (khách báo thủ công cần admin xác nhận),
     * đây là xác nhận tự động — admin/staff chỉ cần được thông báo.
     */
    public void notifyDepositPaid(ReservationEntity r) {
        String customerName = r.getUser() != null ? r.getUser().getFullName() : r.getGuestName();
        String amountText = formatMoney(r.getDepositAmount());

        NotificationEntity notification = new NotificationEntity();
        notification.setType("DEPOSIT_PAID");
        notification.setTitle("Đã nhận tiền cọc giữ bàn");
        notification.setMessage(customerName + " vừa thanh toán cọc " + amountText
                + " qua PayOS cho đặt bàn " + r.getReservationCode() + ".");
        notification.setTargetRole(ROLE_ADMIN);
        notification.setTargetUser(null);
        notification.setReferenceType("RESERVATION");
        notification.setReferenceId(r.getId());
        notification.setReadStatus(false);
        notificationRepository.save(notification);

        try {
            messagingTemplate.convertAndSend("/topic/admin/orders", Map.of(
                    "type", "DEPOSIT_PAID",
                    "title", "Đã nhận tiền cọc",
                    "message", customerName + " thanh toán cọc " + amountText
                            + " — đặt bàn " + r.getReservationCode(),
                    "orderCode", r.getReservationCode(),
                    "reservationId", r.getId(),
                    "amount", r.getDepositAmount() == null ? 0 : r.getDepositAmount().longValue()
            ));
        } catch (Exception ignored) {}

        // Thông báo cho khách hàng
        if (r.getUser() != null) {
            NotificationEntity custNotif = new NotificationEntity();
            custNotif.setType("DEPOSIT_PAID");
            custNotif.setTitle("Thanh toán cọc thành công");
            custNotif.setMessage("Bạn đã thanh toán cọc " + amountText
                    + " cho đặt bàn " + r.getReservationCode()
                    + " thành công. Nhà hàng đã nhận được khoản cọc.");
            custNotif.setTargetRole(ROLE_CUSTOMER);
            custNotif.setTargetUser(r.getUser());
            custNotif.setReferenceType("RESERVATION");
            custNotif.setReferenceId(r.getId());
            custNotif.setReadStatus(false);
            notificationRepository.save(custNotif);
        }
    }



    /**
     * Khách hàng tự hủy đặt bàn -> LƯU thông báo cho admin/staff (hiện ở chuông)
     * và đẩy real-time, đồng thời báo cho khách. Trạng thái gửi kèm là CANCELLED.
     */
    public void notifyReservationCancelledByCustomer(ReservationEntity r) {
        String customerName = r.getUser() != null ? r.getUser().getFullName() : r.getGuestName();

        // ── Lưu + đẩy cho ADMIN/STAFF ──
        NotificationEntity adminNotif = new NotificationEntity();
        adminNotif.setType("RESERVATION_CANCELLED");
        adminNotif.setTitle("Khách hủy đặt bàn");
        adminNotif.setMessage((customerName == null ? "Khách" : customerName)
                + " vừa hủy lượt đặt bàn " + r.getReservationCode()
                + (r.getTable() != null ? " (bàn " + r.getTable().getTableNumber() + ")" : "") + ".");
        adminNotif.setTargetRole(ROLE_ADMIN);
        adminNotif.setTargetUser(null);
        adminNotif.setReferenceType("RESERVATION");
        adminNotif.setReferenceId(r.getId());
        adminNotif.setReadStatus(false);
        notificationRepository.save(adminNotif);

        try {
            messagingTemplate.convertAndSend("/topic/admin/orders", Map.of(
                    "type", "RESERVATION_CANCELLED",
                    "title", "Khách hủy đặt bàn",
                    "message", (customerName == null ? "Khách" : customerName)
                            + " hủy đặt bàn " + r.getReservationCode(),
                    "reservationId", r.getId(),
                    "orderCode", r.getReservationCode(),
                    "newStatus", "CANCELLED"
            ));
        } catch (Exception ignored) {}

        // ── Báo cho khách hàng ──
        if (r.getUser() != null) {
            NotificationEntity custNotif = new NotificationEntity();
            custNotif.setType("RESERVATION_STATUS_UPDATED");
            custNotif.setTitle("Đã hủy đặt bàn");
            custNotif.setMessage("Lượt đặt bàn " + r.getReservationCode()
                    + " đã được hủy theo yêu cầu của bạn.");
            custNotif.setTargetRole(ROLE_CUSTOMER);
            custNotif.setTargetUser(r.getUser());
            custNotif.setReferenceType("RESERVATION");
            custNotif.setReferenceId(r.getId());
            custNotif.setReadStatus(false);
            notificationRepository.save(custNotif);

            try {
                messagingTemplate.convertAndSendToUser(r.getUser().getEmail(), "/queue/orders", Map.of(
                        "type", "RESERVATION_STATUS_CHANGED",
                        "title", "Đã hủy đặt bàn",
                        "message", "Đặt bàn " + r.getReservationCode() + " đã được hủy.",
                        "orderCode", r.getReservationCode(),
                        "orderId", r.getId(),
                        "newStatus", "CANCELLED"
                ));
            } catch (Exception ignored) {}
        }
    }

    /**
     * Hệ thống tự hủy đặt bàn do khách KHÔNG thanh toán cọc trong thời gian quy định.
     * Báo cho admin/staff (lưu + đẩy) và cho khách hàng.
     */
    public void notifyReservationAutoCancelled(ReservationEntity r) {
        String customerName = r.getUser() != null ? r.getUser().getFullName() : r.getGuestName();

        NotificationEntity adminNotif = new NotificationEntity();
        adminNotif.setType("RESERVATION_CANCELLED");
        adminNotif.setTitle("Tự hủy đặt bàn quá hạn thanh toán");
        adminNotif.setMessage("Lượt đặt bàn " + r.getReservationCode() + " của "
                + (customerName == null ? "khách" : customerName)
                + " đã tự hủy do không thanh toán cọc đúng hạn.");
        adminNotif.setTargetRole(ROLE_ADMIN);
        adminNotif.setTargetUser(null);
        adminNotif.setReferenceType("RESERVATION");
        adminNotif.setReferenceId(r.getId());
        adminNotif.setReadStatus(false);
        notificationRepository.save(adminNotif);

        try {
            messagingTemplate.convertAndSend("/topic/admin/orders", Map.of(
                    "type", "RESERVATION_CANCELLED",
                    "title", "Tự hủy đặt bàn quá hạn",
                    "message", "Đặt bàn " + r.getReservationCode() + " tự hủy do quá hạn thanh toán cọc.",
                    "reservationId", r.getId(),
                    "orderCode", r.getReservationCode(),
                    "newStatus", "CANCELLED"
            ));
        } catch (Exception ignored) {}

        if (r.getUser() != null) {
            NotificationEntity custNotif = new NotificationEntity();
            custNotif.setType("RESERVATION_STATUS_UPDATED");
            custNotif.setTitle("Đặt bàn đã bị hủy");
            custNotif.setMessage("Lượt đặt bàn " + r.getReservationCode()
                    + " đã bị hủy do chưa thanh toán cọc trong thời gian quy định.");
            custNotif.setTargetRole(ROLE_CUSTOMER);
            custNotif.setTargetUser(r.getUser());
            custNotif.setReferenceType("RESERVATION");
            custNotif.setReferenceId(r.getId());
            custNotif.setReadStatus(false);
            notificationRepository.save(custNotif);

            try {
                messagingTemplate.convertAndSendToUser(r.getUser().getEmail(), "/queue/orders", Map.of(
                        "type", "RESERVATION_STATUS_CHANGED",
                        "title", "Đặt bàn đã bị hủy",
                        "message", "Đặt bàn " + r.getReservationCode() + " bị hủy do quá hạn thanh toán cọc.",
                        "orderCode", r.getReservationCode(),
                        "orderId", r.getId(),
                        "newStatus", "CANCELLED"
                ));
            } catch (Exception ignored) {}
        }
    }

    private String getReservationStatusText(String status) {
        if (status == null || status.isBlank()) return "Chưa xác định";
        return switch (status.toUpperCase()) {
            case "PENDING" -> "Chờ xác nhận";
            case "CONFIRMED" -> "Đã xác nhận";
            case "SEATED" -> "Đã nhận bàn";
            case "COMPLETED" -> "Hoàn tất";
            case "CANCELLED" -> "Đã hủy";
            case "NO_SHOW" -> "Khách không đến";
            default -> status;
        };
    }

    private String formatTime(java.time.LocalDateTime t) {
        if (t == null) return "";
        return t.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
    }

    private String formatMoney(java.math.BigDecimal amount) {
        if (amount == null) {
            return "0đ";
        }

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return formatter.format(amount);
    }
}
