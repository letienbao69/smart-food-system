package com.food.smart_food_system.Service;

import com.food.smart_food_system.DTO.Wishlist.WishlistResponse;
import com.food.smart_food_system.Entity.FoodEntity;
import com.food.smart_food_system.Entity.UserEntity;
import com.food.smart_food_system.Entity.WishlistEntity;
import com.food.smart_food_system.Exception.BusinessException;
import com.food.smart_food_system.Exception.ResourceNotFoundException;
import com.food.smart_food_system.Repository.FoodRepository;
import com.food.smart_food_system.Repository.UserRepository;
import com.food.smart_food_system.Repository.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final FoodRepository foodRepository;

    public WishlistService(WishlistRepository wishlistRepository,
                           UserRepository userRepository,
                           FoodRepository foodRepository) {
        this.wishlistRepository = wishlistRepository;
        this.userRepository = userRepository;
        this.foodRepository = foodRepository;
    }

    @Transactional(readOnly = true)
    public List<WishlistResponse> getMyWishlist(String email) {
        UserEntity user = findUser(email);
        return wishlistRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public WishlistResponse addToWishlist(String email, Long foodId) {
        UserEntity user = findUser(email);
        FoodEntity food = foodRepository.findById(foodId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy món ăn"));

        if (wishlistRepository.existsByUserIdAndFoodId(user.getId(), foodId)) {
            throw new BusinessException("Món ăn này đã có trong danh sách yêu thích");
        }

        WishlistEntity entity = WishlistEntity.builder()
                .user(user)
                .food(food)
                .build();
        WishlistEntity saved = wishlistRepository.save(entity);
        return mapToResponse(saved);
    }

    @Transactional
    public void removeFromWishlist(String email, Long foodId) {
        UserEntity user = findUser(email);
        // Idempotent: if the row isn't there, we're already in the desired
        // state — don't make the client deal with an error.
        if (wishlistRepository.existsByUserIdAndFoodId(user.getId(), foodId)) {
            wishlistRepository.deleteByUserIdAndFoodId(user.getId(), foodId);
        }
    }

    @Transactional(readOnly = true)
    public boolean checkInWishlist(String email, Long foodId) {
        UserEntity user = findUser(email);
        return wishlistRepository.existsByUserIdAndFoodId(user.getId(), foodId);
    }

    @Transactional(readOnly = true)
    public long countWishlist(String email) {
        UserEntity user = findUser(email);
        return wishlistRepository.countByUserId(user.getId());
    }

    private UserEntity findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    private WishlistResponse mapToResponse(WishlistEntity wishlist) {
        FoodEntity food = wishlist.getFood();
        return WishlistResponse.builder()
                .id(wishlist.getId())
                .foodId(food.getId())
                .foodName(food.getName())
                .description(food.getDescription())
                .price(food.getPrice())
                .stock(food.getStock())
                .imageUrl(food.getImageUrl())
                .ratingAvg(food.getRatingAvg())
                .createdAt(wishlist.getCreatedAt())
                .calories(food.getCalories())
                .tags(food.getTags())
                .categoryName(food.getCategory() != null ? food.getCategory().getName() : null)
                .status(food.getStatus())
                .build();
    }
}
