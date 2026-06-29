package com.food.smart_food_system.Controller;

import com.food.smart_food_system.Entity.OrderEntity;
import com.food.smart_food_system.Entity.ReservationEntity;
import com.food.smart_food_system.Reponse.ApiResponse;
import com.food.smart_food_system.Repository.OrderRepository;
import com.food.smart_food_system.Repository.ReservationRepository;
import com.food.smart_food_system.Service.NotificationService;
import com.food.smart_food_system.Service.PayOSService;
import com.food.smart_food_system.Service.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Endpoint tích hợp cổng thanh toán PayOS.
 *
 * Luồng đặt cọc giữ bàn qua PayOS:
 *   1) FE gọi POST /api/payment/payos/reservation/{id} để xin link/QR thanh toán.
 *   2) Khách quét QR hoặc bấm checkoutUrl để chuyển khoản.
 *   3) PayOS gọi webhook POST /api/payment/payos/webhook về máy chủ này
 *      kèm signature; máy chủ xác thực rồi cập nhật trạng thái đặt cọc.
 *
 * Cấu hình trong application.properties:
 *   payos.client-id, payos.api-key, payos.checksum-key
 *   payos.return-url, payos.cancel-url
 */
@RestController
@RequestMapping("/api/payment/payos")
public class PayOSController {

    private static final Logger log = LoggerFactory.getLogger(PayOSController.class);

    private final PayOSService payOS;
    private final ReservationRepository reservationRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final ReservationService reservationService;

    public PayOSController(
            PayOSService payOS,
            ReservationRepository reservationRepository,
            OrderRepository orderRepository,
            NotificationService notificationService,
            ReservationService reservationService
    ) {
        this.payOS = payOS;
        this.reservationRepository = reservationRepository;
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
        this.reservationService = reservationService;
    }

    /** Tạo link/QR thanh toán đặt cọc cho lượt đặt bàn. */
    @PostMapping("/reservation/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createForReservation(@PathVariable Long id) {
        ReservationEntity r = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lượt đặt bàn"));
        if (r.getDepositAmount() == null || r.getDepositAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Lượt đặt bàn này không yêu cầu đặt cọc.");
        }

        long orderCode = buildOrderCode(r);
        long amount = r.getDepositAmount().longValue();
        String desc = "Coc dat ban " + (r.getReservationCode() == null ? r.getId() : r.getReservationCode());

        Map<String, Object> result = payOS.createPaymentLink(orderCode, amount, desc);

        // Lưu paymentLinkId vào ghi chú để webhook tra ngược nhanh hơn (tùy chọn)
        r.setDepositStatus("PENDING");
        r.setPaymentMethod("PAYOS");
        r.setDepositRequestedAt(java.time.LocalDateTime.now()); // mốc để tự hủy nếu quá hạn thanh toán
        reservationRepository.save(r);

        result.put("reservationId", r.getId());
        return ResponseEntity.ok(ApiResponse.success("Tạo link thanh toán PayOS thành công", result));
    }

    /**
     * Webhook PayOS gọi về sau khi thanh toán thành công.
     * Trả 200 kể cả khi xử lý lỗi để PayOS không retry liên tục.
     */
    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestBody Map<String, Object> payload) {
        log.info("[PayOS Webhook] Nhận: {}", payload);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            String signature = String.valueOf(payload.getOrDefault("signature", ""));

            if (data == null) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Empty data — likely a webhook verification ping"));
            }

            // 1) Xác thực chữ ký
            if (!payOS.verifyWebhookSignature(data, signature)) {
                log.warn("[PayOS Webhook] Chữ ký không hợp lệ");
                return ResponseEntity.ok(Map.of("success", false, "message", "Invalid signature"));
            }

            // 2) Chỉ xử lý khi PayOS báo thành công (code "00")
            String code = String.valueOf(data.getOrDefault("code", ""));
            if (!"00".equals(code)) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Ignored: code=" + code));
            }

            long orderCode = toLong(data.get("orderCode"));
            long amount = toLong(data.get("amount"));

            // 3) Map orderCode -> reservation / order
            ReservationEntity reservation = findReservationByOrderCode(orderCode);
            if (reservation != null) {
                // Tự động ghi nhận đã cọc + tự xác nhận đặt bàn + thông báo real-time (idempotent)
                reservationService.markDepositPaidByGateway(reservation.getId());
                log.info("[PayOS] ✅ Đã nhận cọc & tự xác nhận reservation {} ({} VND)", reservation.getId(), amount);
                return ResponseEntity.ok(Map.of("success", true, "message", "OK reservation " + reservation.getId()));
            }

            // Hoặc orderCode thuộc về một đơn món (ORD-...)
            OrderEntity order = findOrderByOrderCode(orderCode);
            if (order != null) {
                if (!"PAID".equalsIgnoreCase(order.getPaymentStatus())) {
                    String old = order.getPaymentStatus();
                    order.setPaymentStatus("PAID");
                    orderRepository.save(order);
                    notificationService.notifyPaymentStatusChanged(order, old);
                }
                return ResponseEntity.ok(Map.of("success", true, "message", "OK order " + order.getOrderCode()));
            }

            log.warn("[PayOS Webhook] Không tìm thấy reservation/order với orderCode={}", orderCode);
            return ResponseEntity.ok(Map.of("success", false, "message", "Order not found"));

        } catch (Exception e) {
            log.error("[PayOS Webhook] Lỗi xử lý", e);
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Frontend gọi định kỳ để hỏi trạng thái thanh toán (vì webhook PayOS không gọi được về localhost).
     * Nếu PayOS báo đã thanh toán -> tự xác nhận cọc + xác nhận đặt bàn (idempotent) ngay tại đây.
     */
    @GetMapping("/status/{orderCode}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkStatus(@PathVariable long orderCode) {
        String payosStatus = payOS.getPaymentStatus(orderCode);
        long reservationId = orderCode % 1_000_000L;
        ReservationEntity r = reservationRepository.findById(reservationId).orElse(null);

        Map<String, Object> out = new HashMap<>();
        out.put("payosStatus", payosStatus);
        if (r != null) {
            if ("PAID".equalsIgnoreCase(payosStatus) && !"PAID".equalsIgnoreCase(r.getDepositStatus())) {
                reservationService.markDepositPaidByGateway(reservationId);
                r = reservationRepository.findById(reservationId).orElse(r);
            }
            out.put("reservationId", reservationId);
            out.put("depositStatus", r.getDepositStatus());
            out.put("reservationStatus", r.getStatus());
        }
        return ResponseEntity.ok(ApiResponse.success("OK", out));
    }

    // ---------- helpers ----------

    // Bộ đếm tăng dần đảm bảo orderCode DUY NHẤT cho mỗi lần tạo link, kể cả nhiều
    // yêu cầu trong cùng một giây (tránh PayOS lỗi 231 - đơn thanh toán đã tồn tại).
    // Khởi tạo theo epoch giây để không trùng với các orderCode đã tạo trước khi khởi động lại.
    private static final java.util.concurrent.atomic.AtomicLong ORDER_SEQ =
            new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis() / 1000L);

    /** orderCode = số tăng dần * 1_000_000 + id đặt bàn. Giải mã ngược: id = orderCode % 1_000_000. */
    private long buildOrderCode(ReservationEntity r) {
        long base = ORDER_SEQ.getAndIncrement();
        return base * 1_000_000L + (r.getId() % 1_000_000L);
    }

    private ReservationEntity findReservationByOrderCode(long orderCode) {
        // id đặt bàn nằm ở 6 chữ số cuối của orderCode (xem buildOrderCode).
        long id = orderCode % 1_000_000L;
        if (id <= 0) return null;
        return reservationRepository.findById(id).orElse(null);
    }

    private OrderEntity findOrderByOrderCode(long orderCode) {
        // Mở rộng sau nếu cần map orderCode <-> order
        return null;
    }

    private long toLong(Object v) {
        if (v == null) return 0L;
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception ignored) { return 0L; }
    }
}
