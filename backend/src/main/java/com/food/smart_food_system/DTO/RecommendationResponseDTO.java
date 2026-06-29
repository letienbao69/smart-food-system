package com.food.smart_food_system.DTO;

import java.util.List;

public class RecommendationResponseDTO {

    private Long userId;
    private String source;
    private List<Long> recommendedIds;
    private List<RecommendationFoodDTO> items;

    public RecommendationResponseDTO() {
    }

    public RecommendationResponseDTO(
            Long userId,
            String source,
            List<Long> recommendedIds,
            List<RecommendationFoodDTO> items
    ) {
        this.userId = userId;
        this.source = source;
        this.recommendedIds = recommendedIds;
        this.items = items;
    }

    public Long getUserId() {
        return userId;
    }

    public String getSource() {
        return source;
    }

    public List<Long> getRecommendedIds() {
        return recommendedIds;
    }

    public List<RecommendationFoodDTO> getItems() {
        return items;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setRecommendedIds(List<Long> recommendedIds) {
        this.recommendedIds = recommendedIds;
    }

    public void setItems(List<RecommendationFoodDTO> items) {
        this.items = items;
    }
}