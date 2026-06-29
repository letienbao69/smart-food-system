package com.food.smart_food_system.Controller;

import com.food.smart_food_system.DTO.chatbot.ChatRequest;
import com.food.smart_food_system.DTO.chatbot.ChatResponse;
import com.food.smart_food_system.Entity.ChatbotLogEntity;
import com.food.smart_food_system.Entity.UserEntity;
import com.food.smart_food_system.Reponse.ApiResponse;
import com.food.smart_food_system.Repository.ChatbotLogRepository;
import com.food.smart_food_system.Service.ChatbotService;
import com.food.smart_food_system.Service.CustomUserDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Chatbot", description = "Trợ lý AI hội thoại")
@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final CustomUserDetailsService userDetailsService;
    private final ChatbotLogRepository chatbotLogRepository;

    public ChatbotController(ChatbotService chatbotService,
                             CustomUserDetailsService userDetailsService,
                             ChatbotLogRepository chatbotLogRepository) {
        this.chatbotService = chatbotService;
        this.userDetailsService = userDetailsService;
        this.chatbotLogRepository = chatbotLogRepository;
    }

    @Operation(summary = "Gửi câu hỏi tới chatbot AI")
    @PostMapping("/message")
    public ResponseEntity<?> chat(Authentication auth, @RequestBody ChatRequest request) {
        UserEntity user = userDetailsService.getUserByEmail(auth.getName());
        ChatResponse response = chatbotService.chat(user.getId(), request.getMessage());
        return ResponseEntity.ok(ApiResponse.success("Chatbot trả lời thành công", response));
    }

    @Operation(summary = "Health-check chatbot endpoint")
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    @Operation(summary = "Lịch sử chat của người dùng hiện tại")
    @GetMapping("/my-history")
    public ResponseEntity<?> myHistory(Authentication auth) {
        UserEntity user = userDetailsService.getUserByEmail(auth.getName());
        List<ChatbotLogEntity> logs = chatbotLogRepository.findAll(
                Sort.by(Sort.Direction.DESC, "id"));
        // Filter to the current user only (kept simple; could be a repo method)
        List<ChatbotLogEntity> mine = logs.stream()
                .filter(l -> user.getId().equals(l.getUserId()))
                .limit(50)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("OK", mine));
    }
}
