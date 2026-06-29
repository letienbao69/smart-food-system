package com.food.smart_food_system.DTO.chatbot;

import lombok.*;

import java.math.BigDecimal;

/** Món ăn được trợ lý AI gợi ý kèm theo câu trả lời (để hiển thị card + ảnh). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSuggestedFood {
    private Long id;
    private String name;
    private String imageUrl;
    private BigDecimal price;
    private String categoryName;
    private Integer calories;
}
