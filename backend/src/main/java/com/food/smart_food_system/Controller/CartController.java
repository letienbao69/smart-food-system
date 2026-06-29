package com.food.smart_food_system.Controller;

import com.food.smart_food_system.DTO.AddToCartRequest;
import com.food.smart_food_system.DTO.CartDTO;
import com.food.smart_food_system.DTO.UpdateCartItemRequest;
import com.food.smart_food_system.Reponse.ApiResponse;
import com.food.smart_food_system.Service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService service;
    public CartController(CartService service) { this.service = service; }

    @GetMapping public ResponseEntity<ApiResponse<CartDTO>> getMyCart(Authentication authentication) { return ResponseEntity.ok(ApiResponse.success("OK", service.getMyCart(authentication.getName()))); }
    @PostMapping("/items") public ResponseEntity<ApiResponse<CartDTO>> add(Authentication authentication, @Valid @RequestBody AddToCartRequest request) { return ResponseEntity.ok(ApiResponse.success("Thêm vào giỏ hàng thành công", service.addToCart(authentication.getName(), request))); }
    @PutMapping("/items/{itemId}") public ResponseEntity<ApiResponse<CartDTO>> update(Authentication authentication, @PathVariable Long itemId, @Valid @RequestBody UpdateCartItemRequest request) { return ResponseEntity.ok(ApiResponse.success("Cập nhật giỏ hàng thành công", service.updateItem(authentication.getName(), itemId, request))); }
    @DeleteMapping("/items/{itemId}") public ResponseEntity<ApiResponse<Object>> remove(Authentication authentication, @PathVariable Long itemId) { service.removeItem(authentication.getName(), itemId); return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm khỏi giỏ hàng thành công", null)); }
    @DeleteMapping("/clear") public ResponseEntity<ApiResponse<Object>> clear(Authentication authentication) { service.clearCart(authentication.getName()); return ResponseEntity.ok(ApiResponse.success("Xóa toàn bộ giỏ hàng thành công", null)); }
}
