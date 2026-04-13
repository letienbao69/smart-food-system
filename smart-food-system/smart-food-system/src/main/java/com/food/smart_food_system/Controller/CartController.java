package com.food.smart_food_system.Controller;

import com.food.smart_food_system.DTO.AddToCartRequest;
import com.food.smart_food_system.DTO.CartDTO;
import com.food.smart_food_system.DTO.UpdateCartItemRequest;
import com.food.smart_food_system.Service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carts")
@CrossOrigin(origins = "http://localhost:5173")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping("/{userId}")
    public ResponseEntity<CartDTO> getCart(@PathVariable Long userId) {
        return ResponseEntity.ok(cartService.getCartByUserId(userId));
    }

    @PostMapping("/{userId}/items")
    public ResponseEntity<?> addToCart(@PathVariable Long userId,
                                       @RequestBody AddToCartRequest request) {
        try {
            return ResponseEntity.ok(cartService.addToCart(userId, request));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PutMapping("/{userId}/items/{foodId}")
    public ResponseEntity<?> updateCartItem(@PathVariable Long userId,
                                            @PathVariable Long foodId,
                                            @RequestBody UpdateCartItemRequest request) {
        try {
            return ResponseEntity.ok(cartService.updateCartItem(userId, foodId, request));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @DeleteMapping("/{userId}/items/{foodId}")
    public ResponseEntity<?> removeCartItem(@PathVariable Long userId,
                                            @PathVariable Long foodId) {
        try {
            return ResponseEntity.ok(cartService.removeCartItem(userId, foodId));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<String> clearCart(@PathVariable Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok("Đã xóa toàn bộ giỏ hàng");
    }
}