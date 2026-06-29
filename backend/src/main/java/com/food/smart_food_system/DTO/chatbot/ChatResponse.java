package com.food.smart_food_system.DTO.chatbot;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private Long logId;
    private String message;
    private String answer;
    private java.util.List<ChatSuggestedFood> suggestedFoods;
}
