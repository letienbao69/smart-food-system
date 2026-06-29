package com.food.smart_food_system.DTO;

import jakarta.validation.constraints.Min;

public class UpdateCartItemRequest {
    @Min(1) private Integer quantity;
    public Integer getQuantity(){return quantity;} public void setQuantity(Integer quantity){this.quantity=quantity;}
}
