package com.food.smart_food_system.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Tích hợp PayOS — tạo link thanh toán và xác thực chữ ký webhook.
 * Tài liệu: https://payos.vn/docs/api
 *
 * Quy tắc tạo signature theo payOS:
 *   - Sắp xếp các trường theo key alphabet.
 *   - Nối "key1=value1&key2=value2&..." (giá trị null → chuỗi rỗng).
 *   - HMAC-SHA256 với checksum key.
 *
 * Với "tạo link thanh toán": chuỗi ký là
 *   amount=...&cancelUrl=...&description=...&orderCode=...&returnUrl=...
 *
 * Với "webhook": ký toàn bộ object data theo alphabet.
 */
@Service
public class PayOSService {

    private static final Logger log = LoggerFactory.getLogger(PayOSService.class);
    private static final String BASE_URL = "https://api-merchant.payos.vn";

    @Value("${payos.client-id:}")
    private String clientId;

    @Value("${payos.api-key:}")
    private String apiKey;

    @Value("${payos.checksum-key:}")
    private String checksumKey;

    @Value("${payos.return-url:http://localhost:5173/payment/success}")
    private String returnUrl;

    @Value("${payos.cancel-url:http://localhost:5173/payment/cancel}")
    private String cancelUrl;

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper json = new ObjectMapper();

    /** Có cấu hình PayOS hợp lệ hay không. */
    public boolean isEnabled() {
        return clientId != null && !clientId.isBlank()
            && apiKey != null && !apiKey.isBlank()
            && checksumKey != null && !checksumKey.isBlank();
    }

    /**
     * Gọi PayOS để tạo link thanh toán.
     * @param orderCode  mã đơn (số nguyên, duy nhất)
     * @param amount     số tiền VND
     * @param description mô tả (tối đa 25 ký tự để khỏi bị cắt)
     * @return map có checkoutUrl, qrCode, paymentLinkId, status...
     */
    public Map<String, Object> createPaymentLink(long orderCode, long amount, String description) {
        if (!isEnabled()) {
            throw new IllegalStateException("PayOS chưa được cấu hình (payos.client-id / api-key / checksum-key).");
        }
        // Mô tả PayOS giới hạn 25 ký tự (với một số trường hợp tài khoản chưa liên kết)
        String desc = description == null ? "" : description;
        if (desc.length() > 25) desc = desc.substring(0, 25);

        // 1) Tạo signature theo thứ tự alphabet
        String dataToSign = "amount=" + amount
                + "&cancelUrl=" + cancelUrl
                + "&description=" + desc
                + "&orderCode=" + orderCode
                + "&returnUrl=" + returnUrl;
        String signature = hmacSha256(dataToSign, checksumKey);

        // 2) Dựng request body
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderCode", orderCode);
        body.put("amount", amount);
        body.put("description", desc);
        body.put("cancelUrl", cancelUrl);
        body.put("returnUrl", returnUrl);
        body.put("signature", signature);

        // 3) Gọi REST
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-client-id", clientId);
        headers.set("x-api-key", apiKey);

        ResponseEntity<String> resp;
        try {
            resp = rest.exchange(
                    BASE_URL + "/v2/payment-requests",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );
        } catch (Exception e) {
            log.error("[PayOS] Gọi tạo link thất bại: {}", e.getMessage());
            throw new RuntimeException("Không gọi được PayOS: " + e.getMessage(), e);
        }

        try {
            JsonNode root = json.readTree(resp.getBody());
            String code = root.path("code").asText();
            if (!"00".equals(code)) {
                String desc2 = root.path("desc").asText();
                throw new RuntimeException("PayOS trả lỗi: " + code + " - " + desc2);
            }
            JsonNode data = root.path("data");
            Map<String, Object> out = new HashMap<>();
            out.put("checkoutUrl", data.path("checkoutUrl").asText());
            out.put("qrCode", data.path("qrCode").asText());
            out.put("paymentLinkId", data.path("paymentLinkId").asText());
            out.put("status", data.path("status").asText());
            out.put("accountNumber", data.path("accountNumber").asText());
            out.put("accountName", data.path("accountName").asText());
            out.put("bin", data.path("bin").asText());
            out.put("amount", data.path("amount").asLong());
            out.put("orderCode", data.path("orderCode").asLong());
            return out;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Không đọc được phản hồi PayOS: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy trạng thái thanh toán của một orderCode từ PayOS.
     * @return "PAID" | "PENDING" | "PROCESSING" | "CANCELLED" | "EXPIRED" hoặc null nếu lỗi.
     */
    public String getPaymentStatus(long orderCode) {
        if (!isEnabled()) return null;
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-client-id", clientId);
        headers.set("x-api-key", apiKey);
        try {
            ResponseEntity<String> resp = rest.exchange(
                    BASE_URL + "/v2/payment-requests/" + orderCode,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            JsonNode root = json.readTree(resp.getBody());
            if (!"00".equals(root.path("code").asText())) return null;
            return root.path("data").path("status").asText();
        } catch (Exception e) {
            log.warn("[PayOS] Không lấy được trạng thái orderCode={}: {}", orderCode, e.getMessage());
            return null;
        }
    }

    /**
     * Xác thực chữ ký webhook do PayOS gửi về.
     * @param data       object "data" trong payload webhook
     * @param signature  giá trị "signature" trong payload webhook
     * @return true nếu khớp, false nếu sai
     */
    public boolean verifyWebhookSignature(Map<String, Object> data, String signature) {
        if (data == null || signature == null || checksumKey == null || checksumKey.isBlank()) return false;
        // Sắp xếp key alphabet, value null/"undefined"/"null" -> "", value là object/array -> JSON-serialize
        TreeMap<String, Object> sorted = new TreeMap<>(data);
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Object> e : sorted.entrySet()) {
            Object v = e.getValue();
            String s;
            if (v == null) s = "";
            else if (v instanceof String) {
                String str = (String) v;
                s = ("undefined".equals(str) || "null".equals(str)) ? "" : str;
            } else if (v instanceof Map || v instanceof Collection) {
                try { s = json.writeValueAsString(v); } catch (Exception ex) { s = String.valueOf(v); }
            } else {
                s = String.valueOf(v);
            }
            if (!first) sb.append('&');
            sb.append(e.getKey()).append('=').append(s);
            first = false;
        }
        String expected = hmacSha256(sb.toString(), checksumKey);
        return expected.equalsIgnoreCase(signature);
    }

    // ---------- helpers ----------
    private static String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : raw) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Không tạo được HMAC-SHA256: " + e.getMessage(), e);
        }
    }
}
