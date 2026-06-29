package com.food.smart_food_system.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class AddToCartRequest {
    @NotNull private Long foodId;
    @Min(1) private Integer quantity;
    public Long getFoodId(){return foodId;} public void setFoodId(Long foodId){this.foodId=foodId;}
    public Integer getQuantity(){return quantity;} public void setQuantity(Integer quantity){this.quantity=quantity;}
}
