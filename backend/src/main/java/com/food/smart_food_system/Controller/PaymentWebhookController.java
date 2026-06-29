package com.food.smart_food_system.Controller;

import com.food.smart_food_system.Entity.OrderEntity;
import com.food.smart_food_system.Repository.OrderRepository;
import com.food.smart_food_system.Service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Nhận webhook từ SePay (https://sepay.vn) khi có giao dịch vào tài khoản.
 *
 * Cấu hình SePay:
 *   URL Webhook: https://{your-domain}/api/payment/webhook
 *   Secret: đặt SEPAY_SECRET trong environment (tùy chọn)
 *
 * Payload SePay gửi về (dạng JSON):
 * {
 *   "id": 123,
 *   "gateway": "VPBank",
 *   "transactionDate": "2024-05-20 14:30:00",
 *   "accountNumber": "0978250838",
 *   "code": "ORD-XXXXXXXX",       ← có thể null nếu SePay không parse được
 *   "content": "SMARTFOOD ORD-XXXXXXXX",
 *   "transferType": "in",          ← chỉ xử lý "in" (nhận tiền)
 *   "transferAmount": 150000,
 *   "referenceCode": "FT123456",
 *   "description": "..."
 * }
 */
@RestController
@RequestMapping("/api/payment/webhook")
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    public PaymentWebhookController(
            OrderRepository orderRepository,
            NotificationService notificationService
    ) {
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<?> receiveWebhook(@RequestBody Map<String, Object> payload) {
        log.info("[Webhook] Received: {}", payload);

        try {
            // Chỉ xử lý giao dịch nhận tiền
            String transferType = String.valueOf(payload.getOrDefault("transferType", ""));
            if (!"in".equalsIgnoreCase(transferType)) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Ignored: not incoming"));
            }

            // Lấy nội dung chuyển khoản để tìm mã đơn hàng
            String content = String.valueOf(payload.getOrDefault("content", ""));
            String code    = String.valueOf(payload.getOrDefault("code", ""));

            // Số tiền giao dịch
            BigDecimal amount = parseBigDecimal(payload.get("transferAmount"));

            // Ưu tiên field "code" của SePay, fallback sang quét content
            OrderEntity order = findOrderFromWebhook(code, content);

            if (order == null) {
                log.warn("[Webhook] No order found for content='{}' code='{}'", content, code);
                return ResponseEntity.ok(Map.of("success", false, "message", "Order not found"));
            }

            // Đã thanh toán rồi — idempotent
            if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Already paid"));
            }

            // Kiểm tra số tiền (cho phép sai lệch ±1000đ do làm tròn)
            if (amount != null && order.getFinalAmount() != null) {
                BigDecimal diff = amount.subtract(order.getFinalAmount()).abs();
                if (diff.compareTo(BigDecimal.valueOf(1000)) > 0) {
                    log.warn("[Webhook] Amount mismatch: paid={} expected={} for order={}",
                            amount, order.getFinalAmount(), order.getOrderCode());
                    // Vẫn xử lý nhưng log warning — uncomment dòng dưới để strict check
                    // return ResponseEntity.ok(Map.of("success", false, "message", "Amount mismatch"));
                }
            }

            // ── Cập nhật trạng thái ──
            String oldPaymentStatus = order.getPaymentStatus();
            order.setPaymentStatus("PAID");
            orderRepository.save(order);

            log.info("[Webhook] ✅ Order {} marked as PAID (was {})", order.getOrderCode(), oldPaymentStatus);

            // ── Thông báo real-time qua WebSocket ──
            notificationService.notifyPaymentStatusChanged(order, oldPaymentStatus);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment confirmed for " + order.getOrderCode()
            ));

        } catch (Exception e) {
            log.error("[Webhook] Error processing webhook", e);
            // Trả về 200 để SePay không retry liên tục
            return ResponseEntity.ok(Map.of("success", false, "message", "Internal error: " + e.getMessage()));
        }
    }

    private OrderEntity findOrderFromWebhook(String code, String content) {
        // 1. Thử field code trực tiếp (SePay tự parse nội dung)
        if (code != null && code.startsWith("ORD-")) {
            var opt = orderRepository.findByOrderCode(code.trim());
            if (opt.isPresent()) return opt.get();
        }

        // 2. Quét nội dung chuyển khoản tìm pattern ORD-XXXXXXXX
        if (content != null && !content.isBlank()) {
            List<OrderEntity> matches = orderRepository.findByOrderCodeInContent(content);
            if (!matches.isEmpty()) return matches.get(0);

            // 3. Regex fallback: tìm ORD- trong content
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("ORD-[A-Z0-9]{8}")
                    .matcher(content.toUpperCase());
            if (m.find()) {
                var opt = orderRepository.findByOrderCode(m.group());
                if (opt.isPresent()) return opt.get();
            }
        }

        return null;
    }

    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) return null;
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
