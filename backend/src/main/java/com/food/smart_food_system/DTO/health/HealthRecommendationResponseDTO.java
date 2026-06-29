package com.food.smart_food_system.DTO.health;

import java.util.List;

/**
 * Response of GET /api/health/recommendations - bundles the BMI analysis
 * together with the list of foods that scored highest.
 */
public class HealthRecommendationResponseDTO {

    private Long userId;
    private HealthAnalysisDTO analysis;
    private List<HealthFoodDTO> recommendations;

    /** Free-form, AI-generated guidance. Empty when Gemini key is not configured. */
    private String aiAdvice;

    public HealthRecommendationResponseDTO() {
    }

    public HealthRecommendationResponseDTO(Long userId,
                                           HealthAnalysisDTO analysis,
                                           List<HealthFoodDTO> recommendations,
                                           String aiAdvice) {
        this.userId = userId;
        this.analysis = analysis;
        this.recommendations = recommendations;
        this.aiAdvice = aiAdvice;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public HealthAnalysisDTO getAnalysis() { return analysis; }
    public void setAnalysis(HealthAnalysisDTO analysis) { this.analysis = analysis; }

    public List<HealthFoodDTO> getRecommendations() { return recommendations; }
    public void setRecommendations(List<HealthFoodDTO> recommendations) { this.recommendations = recommendations; }

    public String getAiAdvice() { return aiAdvice; }
    public void setAiAdvice(String aiAdvice) { this.aiAdvice = aiAdvice; }
}
