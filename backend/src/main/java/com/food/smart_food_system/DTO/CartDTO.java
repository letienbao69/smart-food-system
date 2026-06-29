package com.food.smart_food_system.DTO;

import java.math.BigDecimal;
import java.util.List;

public class CartDTO {
    private Long cartId; private Long userId; private List<CartItemDTO> items; private BigDecimal totalAmount;
    public Long getCartId(){return cartId;} public void setCartId(Long cartId){this.cartId=cartId;}
    public Long getUserId(){return userId;} public void setUserId(Long userId){this.userId=userId;}
    public List<CartItemDTO> getItems(){return items;} public void setItems(List<CartItemDTO> items){this.items=items;}
    public BigDecimal getTotalAmount(){return totalAmount;} public void setTotalAmount(BigDecimal totalAmount){this.totalAmount=totalAmount;}
}
