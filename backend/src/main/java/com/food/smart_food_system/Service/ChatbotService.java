package com.food.smart_food_system.Service;

import com.food.smart_food_system.DTO.chatbot.ChatResponse;

public interface ChatbotService {
    public ChatResponse chat(Long userId, String userMessage);
}
