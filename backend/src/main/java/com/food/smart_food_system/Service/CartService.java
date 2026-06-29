package com.food.smart_food_system.Service;

import com.food.smart_food_system.DTO.AddToCartRequest;
import com.food.smart_food_system.DTO.CartDTO;
import com.food.smart_food_system.DTO.UpdateCartItemRequest;

public interface CartService {
    CartDTO getMyCart(String email);
    CartDTO addToCart(String email, AddToCartRequest request);
    CartDTO updateItem(String email, Long itemId, UpdateCartItemRequest request);
    void removeItem(String email, Long itemId);
    void clearCart(String email);
}
