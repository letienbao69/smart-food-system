package com.food.smart_food_system.DTO;

import java.math.BigDecimal;

public class OrderItemResponseDTO {
    private Long id; private Long foodId; private String foodName; private Integer quantity; private BigDecimal unitPrice; private BigDecimal subtotal;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getFoodId(){return foodId;} public void setFoodId(Long foodId){this.foodId=foodId;}
    public String getFoodName(){return foodName;} public void setFoodName(String foodName){this.foodName=foodName;}
    public Integer getQuantity(){return quantity;} public void setQuantity(Integer quantity){this.quantity=quantity;}
    public BigDecimal getUnitPrice(){return unitPrice;} public void setUnitPrice(BigDecimal unitPrice){this.unitPrice=unitPrice;}
    public BigDecimal getSubtotal(){return subtotal;} public void setSubtotal(BigDecimal subtotal){this.subtotal=subtotal;}
}
