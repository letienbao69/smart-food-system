package com.food.smart_food_system.DTO;

import java.math.BigDecimal;

public class BestSellingFoodDTO {

    private Long foodId;
    private String foodName;
    private Long totalQuantity;
    private BigDecimal totalRevenue;

    public BestSellingFoodDTO() {
    }

    public BestSellingFoodDTO(Long foodId, String foodName, Long totalQuantity, BigDecimal totalRevenue) {
        this.foodId = foodId;
        this.foodName = foodName;
        this.totalQuantity = totalQuantity;
        this.totalRevenue = totalRevenue;
    }

    public Long getFoodId() {
        return foodId;
    }

    public String getFoodName() {
        return foodName;
    }

    public Long getTotalQuantity() {
        return totalQuantity;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setFoodId(Long foodId) {
        this.foodId = foodId;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public void setTotalQuantity(Long totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }
}