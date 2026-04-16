package com.food.smart_food_system.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderRequest {
    private Long addressId;
    private Long voucherId;
    private String paymentMethod; // COD / VNPAY / MOMO
    private String note;
}