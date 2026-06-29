package com.food.smart_food_system.Service.impl;

import com.food.smart_food_system.DTO.*;
import com.food.smart_food_system.Entity.CartEntity;
import com.food.smart_food_system.Entity.CartItemEntity;
import com.food.smart_food_system.Entity.FoodEntity;
import com.food.smart_food_system.Entity.UserEntity;
import com.food.smart_food_system.Exception.BusinessException;
import com.food.smart_food_system.Exception.ResourceNotFoundException;
import com.food.smart_food_system.Repository.CartItemRepository;
import com.food.smart_food_system.Repository.CartRepository;
import com.food.smart_food_system.Repository.FoodRepository;
import com.food.smart_food_system.Service.CartService;
import com.food.smart_food_system.Service.CustomUserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final FoodRepository foodRepository;
    private final CustomUserDetailsService customUserDetailsService;

    public CartServiceImpl(CartRepository cartRepository, CartItemRepository cartItemRepository,
                           FoodRepository foodRepository, CustomUserDetailsService customUserDetailsService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.foodRepository = foodRepository;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    public CartDTO getMyCart(String email) {
        UserEntity user = customUserDetailsService.getUserByEmail(email);
        CartEntity cart = getOrCreateCart(user);
        return toDto(cart);
    }

    @Override
    public CartDTO addToCart(String email, AddToCartRequest request) {
        UserEntity user = customUserDetailsService.getUserByEmail(email);
        CartEntity cart = getOrCreateCart(user);
        FoodEntity food = foodRepository.findById(request.getFoodId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy món ăn"));
        if (food.getStock() < request.getQuantity()) throw new BusinessException("Số lượng tồn kho không đủ");

        CartItemEntity item = cartItemRepository.findByCartIdAndFoodId(cart.getId(), food.getId()).orElse(null);
        if (item == null) {
            item = new CartItemEntity();
            item.setCart(cart);
            item.setFood(food);
            item.setQuantity(request.getQuantity());
        } else {
            int total = item.getQuantity() + request.getQuantity();
            if (food.getStock() < total) throw new BusinessException("Số lượng tồn kho không đủ");
            item.setQuantity(total);
        }
        int dpct = food.getDiscountPercent() == null ? 0 : food.getDiscountPercent();
        java.math.BigDecimal unit = food.getPrice();
        if (dpct > 0) {
            unit = unit.multiply(java.math.BigDecimal.valueOf(100 - dpct))
                       .divide(java.math.BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
        }
        item.setUnitPrice(unit);
        cartItemRepository.save(item);
        return toDto(cart);
    }

    @Override
    public CartDTO updateItem(String email, Long itemId, UpdateCartItemRequest request) {
        UserEntity user = customUserDetailsService.getUserByEmail(email);
        CartItemEntity item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ"));
        if (!item.getCart().getUser().getId().equals(user.getId())) throw new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ");
        if (item.getFood().getStock() < request.getQuantity()) throw new BusinessException("Số lượng tồn kho không đủ");
        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);
        return toDto(item.getCart());
    }

    @Override
    public void removeItem(String email, Long itemId) {
        UserEntity user = customUserDetailsService.getUserByEmail(email);
        CartItemEntity item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ"));
        if (!item.getCart().getUser().getId().equals(user.getId())) throw new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ");
        cartItemRepository.delete(item);
    }

    @Override
    public void clearCart(String email) {
        UserEntity user = customUserDetailsService.getUserByEmail(email);
        CartEntity cart = getOrCreateCart(user);
        cartItemRepository.deleteByCartId(cart.getId());
    }

    private CartEntity getOrCreateCart(UserEntity user) {
        return cartRepository.findByUserId(user.getId()).orElseGet(() -> {
            CartEntity cart = new CartEntity();
            cart.setUser(user);
            return cartRepository.save(cart);
        });
    }

    private CartDTO toDto(CartEntity cart) {
        List<CartItemDTO> items = cartItemRepository.findByCartId(cart.getId()).stream().map(item -> {
            CartItemDTO dto = new CartItemDTO();
            dto.setId(item.getId());
            dto.setFoodId(item.getFood().getId());
            dto.setFoodName(item.getFood().getName());
            dto.setQuantity(item.getQuantity());
            dto.setUnitPrice(item.getUnitPrice());
            dto.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            dto.setImageUrl(item.getFood().getImageUrl());
            dto.setCalories(item.getFood().getCalories());
            return dto;
        }).toList();

        CartDTO dto = new CartDTO();
        dto.setCartId(cart.getId());
        dto.setUserId(cart.getUser().getId());
        dto.setItems(items);
        dto.setTotalAmount(items.stream().map(CartItemDTO::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add));
        return dto;
    }
}
