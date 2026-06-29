package com.food.smart_food_system.DTO.Wishlist;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistResponse {

    private Long id;

    private Long foodId;

    private String foodName;

    private String description;

    private BigDecimal price;

    private Integer stock;

    private String imageUrl;

    private BigDecimal ratingAvg;

    private LocalDateTime createdAt;

    // Health/nutrition extras so the wishlist card can show calories + tags
    private Integer calories;
    private String tags;
    private String categoryName;
    private String status;
}