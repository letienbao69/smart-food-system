package com.food.smart_food_system.Controller;

import com.food.smart_food_system.DTO.CreateOrderRequest;
import com.food.smart_food_system.DTO.OrderResponseDTO;
import com.food.smart_food_system.DTO.UpdateOrderStatusRequest;
import com.food.smart_food_system.Reponse.ApiResponse;
import com.food.smart_food_system.Service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:5173")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> createOrder(@PathVariable Long userId,
                                                                     @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Đặt hàng thành công", orderService.createOrder(userId, request))
        );
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<List<OrderResponseDTO>>> getOrdersByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(
                ApiResponse.success("Lấy danh sách đơn hàng thành công", orderService.getOrdersByUserId(userId))
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponseDTO>>> getAllOrders() {
        return ResponseEntity.ok(
                ApiResponse.success("Lấy toàn bộ đơn hàng thành công", orderService.getAllOrders())
        );
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(
                ApiResponse.success("Lấy chi tiết đơn hàng thành công", orderService.getOrderById(orderId))
        );
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> updateOrderStatus(@PathVariable Long orderId,
                                                                           @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Cập nhật trạng thái đơn hàng thành công", orderService.updateOrderStatus(orderId, request))
        );
    }
}