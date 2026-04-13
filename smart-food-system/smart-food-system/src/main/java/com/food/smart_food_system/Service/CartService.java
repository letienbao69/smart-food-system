package com.food.smart_food_system.Service;

import com.food.smart_food_system.DTO.AddToCartRequest;
import com.food.smart_food_system.DTO.CartDTO;
import com.food.smart_food_system.DTO.CartItemDTO;
import com.food.smart_food_system.DTO.UpdateCartItemRequest;
import com.food.smart_food_system.Entity.CartEntity;
import com.food.smart_food_system.Entity.CartItemEntity;
import com.food.smart_food_system.Entity.FoodEntity;
import com.food.smart_food_system.Entity.UserEntity;
import com.food.smart_food_system.Repository.CartItemRepository;
import com.food.smart_food_system.Repository.CartRepository;
import com.food.smart_food_system.Repository.FoodRepository;
import com.food.smart_food_system.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FoodRepository foodRepository;

    public CartDTO getCartByUserId(Long userId) {
        CartEntity cart = getOrCreateCart(userId);
        return mapToCartDTO(cart);
    }

    public CartDTO addToCart(Long userId, AddToCartRequest request) {
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new RuntimeException("Số lượng phải lớn hơn 0");
        }

        CartEntity cart = getOrCreateCart(userId);

        FoodEntity food = foodRepository.findById(request.getFoodId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn với id: " + request.getFoodId()));

        CartItemEntity cartItem = cartItemRepository.findByCartIdAndFoodId(cart.getId(), food.getId())
                .orElse(null);

        if (cartItem == null) {
            cartItem = new CartItemEntity();
            cartItem.setCart(cart);
            cartItem.setFood(food);
            cartItem.setQuantity(request.getQuantity());
            cartItem.setUnitPrice(food.getPrice());
        } else {
            cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
        }

        cartItemRepository.save(cartItem);
        return mapToCartDTO(cart);
    }

    public CartDTO updateCartItem(Long userId, Long foodId, UpdateCartItemRequest request) {
        CartEntity cart = getOrCreateCart(userId);

        CartItemEntity cartItem = cartItemRepository.findByCartIdAndFoodId(cart.getId(), foodId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn này trong giỏ hàng"));

        if (request.getQuantity() == null) {
            throw new RuntimeException("Số lượng không được để trống");
        }

        if (request.getQuantity() <= 0) {
            cartItemRepository.delete(cartItem);
        } else {
            cartItem.setQuantity(request.getQuantity());
            cartItemRepository.save(cartItem);
        }

        return mapToCartDTO(cart);
    }

    public CartDTO removeCartItem(Long userId, Long foodId) {
        CartEntity cart = getOrCreateCart(userId);

        CartItemEntity cartItem = cartItemRepository.findByCartIdAndFoodId(cart.getId(), foodId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn này trong giỏ hàng"));

        cartItemRepository.delete(cartItem);
        return mapToCartDTO(cart);
    }

    public void clearCart(Long userId) {
        CartEntity cart = getOrCreateCart(userId);
        cartItemRepository.deleteByCartId(cart.getId());
    }

    private CartEntity getOrCreateCart(Long userId) {
        CartEntity cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart != null) {
            return cart;
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với id: " + userId));

        CartEntity newCart = new CartEntity();
        newCart.setUser(user);
        return cartRepository.save(newCart);
    }

    private CartDTO mapToCartDTO(CartEntity cart) {
        List<CartItemEntity> cartItems = cartItemRepository.findByCartId(cart.getId());

        List<CartItemDTO> itemDTOs = cartItems.stream().map(item -> {
            BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            return new CartItemDTO(
                    item.getFood().getId(),
                    item.getFood().getName(),
                    item.getFood().getImageUrl(),
                    item.getUnitPrice(),
                    item.getQuantity(),
                    subtotal
            );
        }).toList();

        BigDecimal totalAmount = itemDTOs.stream()
                .map(CartItemDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartDTO(
                cart.getId(),
                cart.getUser().getId(),
                itemDTOs,
                totalAmount
        );
    }
}