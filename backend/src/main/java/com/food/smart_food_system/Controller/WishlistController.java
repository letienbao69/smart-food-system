package com.food.smart_food_system.Controller;

import com.food.smart_food_system.DTO.Wishlist.WishlistResponse;
import com.food.smart_food_system.Reponse.ApiResponse;
import com.food.smart_food_system.Service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Wishlist endpoints. Identity is always taken from the JWT (Authentication),
 * never from the URL. This prevents IDOR - users cannot read or mutate
 * someone else's wishlist.
 */
@Tag(name = "Wishlist", description = "Danh sách món yêu thích của người dùng")
@RestController
@RequestMapping("/api/wishlists")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @Operation(summary = "Danh sách món yêu thích của người dùng hiện tại")
    @GetMapping("/me")
    public ResponseEntity<?> getMyWishlist(Authentication auth) {
        List<WishlistResponse> data = wishlistService.getMyWishlist(auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách yêu thích thành công", data));
    }

    @Operation(summary = "Thêm món vào danh sách yêu thích")
    @PostMapping("/food/{foodId}")
    public ResponseEntity<?> addToWishlist(Authentication auth, @PathVariable Long foodId) {
        WishlistResponse data = wishlistService.addToWishlist(auth.getName(), foodId);
        return ResponseEntity.ok(ApiResponse.success("Đã thêm món ăn vào danh sách yêu thích", data));
    }

    @Operation(summary = "Xóa món khỏi danh sách yêu thích")
    @DeleteMapping("/food/{foodId}")
    public ResponseEntity<?> removeFromWishlist(Authentication auth, @PathVariable Long foodId) {
        wishlistService.removeFromWishlist(auth.getName(), foodId);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa món ăn khỏi danh sách yêu thích", null));
    }

    @Operation(summary = "Kiểm tra một món đã có trong wishlist hay chưa")
    @GetMapping("/food/{foodId}/check")
    public ResponseEntity<?> checkWishlist(Authentication auth, @PathVariable Long foodId) {
        boolean liked = wishlistService.checkInWishlist(auth.getName(), foodId);
        return ResponseEntity.ok(Map.of("liked", liked));
    }

    @Operation(summary = "Đếm số món trong wishlist")
    @GetMapping("/count")
    public ResponseEntity<?> count(Authentication auth) {
        long count = wishlistService.countWishlist(auth.getName());
        return ResponseEntity.ok(Map.of("count", count));
    }
}
