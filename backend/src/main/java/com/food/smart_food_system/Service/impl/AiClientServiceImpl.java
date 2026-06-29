package com.food.smart_food_system.Service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.smart_food_system.Service.AiClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiClientServiceImpl implements AiClientService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.api.url}")
    private String apiUrl;

    @Value("${ai.api.key}")
    private String apiKey;

    @Override
    public String ask(String message) {
        // No key set → return a clear, helpful fallback rather than crashing
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("${")) {
            log.warn("AI API key is not set. Returning fallback response.");
            return "Trợ lý AI chưa được cấu hình (thiếu GEMINI_API_KEY). " +
                   "Vui lòng liên hệ quản trị viên để kích hoạt.";
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", apiKey);

            // Build body with sensible generation config so the model returns
            // ĐẦY ĐỦ câu trả lời tiếng Việt, không bị cắt giữa chừng.
            // - maxOutputTokens cao (Gemini 2.5 Flash tiêu thụ "thinking tokens" trước
            //   khi trả output, nếu để 512 sẽ bị cắt ngang).
            // - thinkingBudget=0: tắt reasoning ngầm để dồn toàn bộ quota cho text trả về.
            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "parts", List.of(
                                            Map.of("text", message)
                                    )
                            )
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.7,
                            "maxOutputTokens", 4096,
                            "topP", 0.9,
                            "thinkingConfig", Map.of("thinkingBudget", 0)
                    )
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode textNode = root.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text");

            if (!textNode.isMissingNode() && !textNode.isNull()) {
                String text = textNode.asText().trim();
                if (!text.isBlank()) return text;
            }

            // Look for an error.message field for nicer diagnostics
            JsonNode errMsg = root.path("error").path("message");
            if (!errMsg.isMissingNode()) {
                log.warn("Gemini API returned error: {}", errMsg.asText());
            }

            return "Xin lỗi, tôi chưa thể tạo câu trả lời lúc này. Bạn có thể " +
                   "thử hỏi lại bằng cách khác, hoặc duyệt menu trực tiếp nhé!";
        } catch (Exception e) {
            // CRITICAL: never echo the user's question back on failure.
            log.error("Gemini API call failed: {}", e.getMessage(), e);
            return "Trợ lý AI đang gặp sự cố tạm thời. Vui lòng thử lại sau ít phút. " +
                   "Bạn cũng có thể xem gợi ý món ăn theo BMI tại trang \"Gợi ý AI\".";
        }
    }
}


