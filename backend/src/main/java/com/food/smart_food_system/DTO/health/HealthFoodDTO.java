package com.food.smart_food_system.DTO.health;

import java.math.BigDecimal;
import java.util.List;

public class HealthFoodDTO {

    private Long foodId;
    private String name;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    private String categoryName;
    private BigDecimal ratingAvg;

    private Integer calories;
    private BigDecimal proteinG;
    private BigDecimal fatG;
    private BigDecimal carbsG;

    /** Score in 0..100 - how well the food matches the user's health profile. */
    private double matchScore;

    /** Tags matched by the food, e.g. ["HIGH_PROTEIN", "LOW_SUGAR"]. */
    private List<String> matchedTags;

    /** Vietnamese explanation, e.g. "Giàu đạm, ít đường - phù hợp mục tiêu giảm cân". */
    private String reason;

    public Long getFoodId() { return foodId; }
    public void setFoodId(Long foodId) { this.foodId = foodId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public BigDecimal getRatingAvg() { return ratingAvg; }
    public void setRatingAvg(BigDecimal ratingAvg) { this.ratingAvg = ratingAvg; }

    public Integer getCalories() { return calories; }
    public void setCalories(Integer calories) { this.calories = calories; }

    public BigDecimal getProteinG() { return proteinG; }
    public void setProteinG(BigDecimal proteinG) { this.proteinG = proteinG; }

    public BigDecimal getFatG() { return fatG; }
    public void setFatG(BigDecimal fatG) { this.fatG = fatG; }

    public BigDecimal getCarbsG() { return carbsG; }
    public void setCarbsG(BigDecimal carbsG) { this.carbsG = carbsG; }

    public double getMatchScore() { return matchScore; }
    public void setMatchScore(double matchScore) { this.matchScore = matchScore; }

    public List<String> getMatchedTags() { return matchedTags; }
    public void setMatchedTags(List<String> matchedTags) { this.matchedTags = matchedTags; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
