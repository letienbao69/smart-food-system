package com.food.smart_food_system.Service;

import com.food.smart_food_system.DTO.CreateOrderRequest;
import com.food.smart_food_system.DTO.OrderResponseDTO;
import com.food.smart_food_system.DTO.UpdateOrderStatusRequest;

import java.util.List;

public interface OrderService {
    OrderResponseDTO createOrder(Long userId, CreateOrderRequest request);
    List<OrderResponseDTO> getOrdersByUserId(Long userId);
    List<OrderResponseDTO> getAllOrders();
    OrderResponseDTO getOrderById(Long orderId);
    OrderResponseDTO updateOrderStatus(Long orderId, UpdateOrderStatusRequest request);
}