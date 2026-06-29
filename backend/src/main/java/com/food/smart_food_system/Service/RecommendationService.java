package com.food.smart_food_system.Service;

import com.food.smart_food_system.DTO.RecommendationFoodDTO;
import com.food.smart_food_system.DTO.RecommendationResponseDTO;
import com.food.smart_food_system.Entity.*;
import com.food.smart_food_system.Repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RecommendationService {

    private final FoodRepository foodRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final WishlistRepository wishlistRepository;
    private final CustomUserDetailsService customUserDetailsService;

    public RecommendationService(
            FoodRepository foodRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            WishlistRepository wishlistRepository,
            CustomUserDetailsService customUserDetailsService
    ) {
        this.foodRepository = foodRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.wishlistRepository = wishlistRepository;
        this.customUserDetailsService = customUserDetailsService;
    }

    public RecommendationResponseDTO recommendForCurrentUser(String email, int limit) {
        UserEntity user = customUserDetailsService.getUserByEmail(email);
        return buildRecommendation(user.getId(), limit, false);
    }

    public RecommendationResponseDTO recommendForCurrentCart(String email, int limit) {
        UserEntity user = customUserDetailsService.getUserByEmail(email);
        return buildRecommendation(user.getId(), limit, true);
    }

    public RecommendationResponseDTO recommendForUserId(Long userId, int limit) {
        return buildRecommendation(userId, limit, false);
    }

    private RecommendationResponseDTO buildRecommendation(Long userId, int limit, boolean cartMode) {
        limit = normalizeLimit(limit);

        List<FoodEntity> allFoods = foodRepository.findAll();
        List<OrderEntity> userOrders = orderRepository.findByUserIdOrderByIdDesc(userId);
        List<OrderEntity> allOrders = orderRepository.findAll();
        List<CartItemEntity> cartItems = getCartItems(userId);
        List<WishlistEntity> wishlists = wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId);

        Set<Long> cartFoodIds = cartItems.stream()
                .map(CartItemEntity::getFood)
                .filter(Objects::nonNull)
                .map(FoodEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> wishlistFoodIds = wishlists.stream()
                .map(WishlistEntity::getFood)
                .filter(Objects::nonNull)
                .map(FoodEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, FoodEntity> foodMap = allFoods.stream()
                .filter(food -> food.getId() != null)
                .collect(Collectors.toMap(FoodEntity::getId, food -> food, (a, b) -> a));

        Map<Long, Integer> userFoodFrequency = new HashMap<>();
        Map<Long, Integer> categoryFrequency = new HashMap<>();

        for (OrderEntity order : userOrders) {
            List<OrderItemEntity> items = orderItemRepository.findByOrderId(order.getId());

            for (OrderItemEntity item : items) {
                FoodEntity food = item.getFood();

                if (food == null || food.getId() == null) {
                    continue;
                }

                int quantity = safeQuantity(item.getQuantity());

                userFoodFrequency.merge(food.getId(), quantity, Integer::sum);

                Long categoryId = getCategoryId(food);
                if (categoryId != null) {
                    categoryFrequency.merge(categoryId, quantity, Integer::sum);
                }
            }
        }

        for (CartItemEntity item : cartItems) {
            FoodEntity food = item.getFood();

            if (food == null || food.getId() == null) {
                continue;
            }

            Long categoryId = getCategoryId(food);

            if (categoryId != null) {
                categoryFrequency.merge(categoryId, safeQuantity(item.getQuantity()) * 3, Integer::sum);
            }
        }

        for (WishlistEntity wishlist : wishlists) {
            FoodEntity food = wishlist.getFood();

            if (food == null || food.getId() == null) {
                continue;
            }

            Long categoryId = getCategoryId(food);

            if (categoryId != null) {
                categoryFrequency.merge(categoryId, 4, Integer::sum);
            }
        }

        Set<Long> seedFoodIds = buildSeedFoods(cartMode, cartFoodIds, wishlistFoodIds, userFoodFrequency);

        Map<Long, Integer> globalPopularity = countGlobalPopularity(allOrders);
        Map<Long, Integer> coBoughtScore = calculateCoBoughtScore(allOrders, seedFoodIds);

        List<ScoredFood> scoredFoods = new ArrayList<>();

        for (FoodEntity food : allFoods) {
            if (!isAvailable(food)) {
                continue;
            }

            Long foodId = food.getId();

            if (foodId == null) {
                continue;
            }

            if (cartMode && cartFoodIds.contains(foodId)) {
                continue;
            }

            double score = 0;
            String reason = "Món phổ biến trong hệ thống";

            Integer boughtTogether = coBoughtScore.get(foodId);
            if (boughtTogether != null && boughtTogether > 0) {
                score += boughtTogether * 10.0;
                reason = "Thường được mua kèm với món bạn đang quan tâm";
            }

            Long categoryId = getCategoryId(food);
            if (categoryId != null && categoryFrequency.containsKey(categoryId)) {
                score += categoryFrequency.get(categoryId) * 3.0;

                if (score < 10.0) {
                    reason = "Cùng nhóm món với sở thích của bạn";
                }
            }

            Integer boughtBefore = userFoodFrequency.get(foodId);
            if (boughtBefore != null && boughtBefore > 0) {
                score += boughtBefore * 4.0;

                if (!cartMode) {
                    reason = "Bạn từng mua món này trước đây";
                }
            }

            if (wishlistFoodIds.contains(foodId)) {
                score += 8.0;
                reason = "Món nằm trong danh sách yêu thích của bạn";
            }

            score += Math.min(globalPopularity.getOrDefault(foodId, 0), 30) * 1.2;
            score += safeRating(food).doubleValue() * 1.5;

            if (food.getStock() != null && food.getStock() > 0) {
                score += 1.0;
            }

            if (score > 0) {
                scoredFoods.add(new ScoredFood(food, score, reason));
            }
        }

        List<RecommendationFoodDTO> result = scoredFoods.stream()
                .sorted(Comparator.comparingDouble(ScoredFood::score).reversed())
                .limit(limit)
                .map(item -> toDto(item.food(), item.score(), item.reason()))
                .collect(Collectors.toList());

        if (result.size() < limit) {
            addFallbackFoods(result, allFoods, cartFoodIds, globalPopularity, limit, cartMode);
        }

        String source = resolveSource(cartMode, cartFoodIds, userOrders, wishlistFoodIds);

        List<Long> ids = result.stream()
                .map(RecommendationFoodDTO::getFoodId)
                .toList();

        return new RecommendationResponseDTO(userId, source, ids, result);
    }

    private List<CartItemEntity> getCartItems(Long userId) {
        Optional<CartEntity> cartOptional = cartRepository.findByUserId(userId);

        if (cartOptional.isEmpty()) {
            return new ArrayList<>();
        }

        return cartItemRepository.findByCartId(cartOptional.get().getId());
    }

    private Set<Long> buildSeedFoods(
            boolean cartMode,
            Set<Long> cartFoodIds,
            Set<Long> wishlistFoodIds,
            Map<Long, Integer> userFoodFrequency
    ) {
        Set<Long> seedFoodIds = new HashSet<>();

        if (cartMode && !cartFoodIds.isEmpty()) {
            seedFoodIds.addAll(cartFoodIds);
            return seedFoodIds;
        }

        if (!wishlistFoodIds.isEmpty()) {
            seedFoodIds.addAll(wishlistFoodIds);
        }

        userFoodFrequency.entrySet()
                .stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> seedFoodIds.add(entry.getKey()));

        return seedFoodIds;
    }

    private Map<Long, Integer> calculateCoBoughtScore(List<OrderEntity> orders, Set<Long> seedFoodIds) {
        Map<Long, Integer> result = new HashMap<>();

        if (seedFoodIds == null || seedFoodIds.isEmpty()) {
            return result;
        }

        for (OrderEntity order : orders) {
            List<OrderItemEntity> items = orderItemRepository.findByOrderId(order.getId());

            Set<Long> orderFoodIds = items.stream()
                    .map(OrderItemEntity::getFood)
                    .filter(Objects::nonNull)
                    .map(FoodEntity::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            boolean hasSeedFood = orderFoodIds.stream().anyMatch(seedFoodIds::contains);

            if (!hasSeedFood) {
                continue;
            }

            for (Long foodId : orderFoodIds) {
                if (!seedFoodIds.contains(foodId)) {
                    result.merge(foodId, 1, Integer::sum);
                }
            }
        }

        return result;
    }

    private Map<Long, Integer> countGlobalPopularity(List<OrderEntity> orders) {
        Map<Long, Integer> result = new HashMap<>();

        for (OrderEntity order : orders) {
            List<OrderItemEntity> items = orderItemRepository.findByOrderId(order.getId());

            for (OrderItemEntity item : items) {
                FoodEntity food = item.getFood();

                if (food == null || food.getId() == null) {
                    continue;
                }

                result.merge(food.getId(), safeQuantity(item.getQuantity()), Integer::sum);
            }
        }

        return result;
    }

    private void addFallbackFoods(
            List<RecommendationFoodDTO> result,
            List<FoodEntity> allFoods,
            Set<Long> cartFoodIds,
            Map<Long, Integer> globalPopularity,
            int limit,
            boolean cartMode
    ) {
        Set<Long> existedIds = result.stream()
                .map(RecommendationFoodDTO::getFoodId)
                .collect(Collectors.toSet());

        List<RecommendationFoodDTO> fallback = allFoods.stream()
                .filter(this::isAvailable)
                .filter(food -> food.getId() != null)
                .filter(food -> !existedIds.contains(food.getId()))
                .filter(food -> !cartMode || !cartFoodIds.contains(food.getId()))
                .sorted((a, b) -> {
                    double scoreA = Math.min(globalPopularity.getOrDefault(a.getId(), 0), 30)
                            + safeRating(a).doubleValue();
                    double scoreB = Math.min(globalPopularity.getOrDefault(b.getId(), 0), 30)
                            + safeRating(b).doubleValue();

                    return Double.compare(scoreB, scoreA);
                })
                .limit(limit - result.size())
                .map(food -> toDto(food, 1.0, "Gợi ý dự phòng theo độ phổ biến"))
                .toList();

        result.addAll(fallback);
    }

    private RecommendationFoodDTO toDto(FoodEntity food, Double score, String reason) {
        RecommendationFoodDTO dto = new RecommendationFoodDTO();

        dto.setId(food.getId());
        dto.setFoodId(food.getId());
        dto.setName(food.getName());
        dto.setDescription(food.getDescription());
        dto.setPrice(food.getPrice());
        dto.setImageUrl(food.getImageUrl());
        dto.setStock(food.getStock());
        dto.setStatus(food.getStatus());
        dto.setRatingAvg(food.getRatingAvg());
        dto.setScore(score);
        dto.setReason(reason);

        if (food.getCategory() != null) {
            dto.setCategoryId(food.getCategory().getId());
            dto.setCategoryName(food.getCategory().getName());
        }

        return dto;
    }

    private String resolveSource(
            boolean cartMode,
            Set<Long> cartFoodIds,
            List<OrderEntity> userOrders,
            Set<Long> wishlistFoodIds
    ) {
        if (cartMode && !cartFoodIds.isEmpty()) {
            return "CART_BASED_RECOMMENDATION";
        }

        if (!userOrders.isEmpty()) {
            return "HISTORY_BASED_RECOMMENDATION";
        }

        if (!wishlistFoodIds.isEmpty()) {
            return "WISHLIST_BASED_RECOMMENDATION";
        }

        return "POPULAR_FALLBACK";
    }

    private int normalizeLimit(int limit) {
        if (limit < 1) {
            return 8;
        }

        return Math.min(limit, 30);
    }

    private Long getCategoryId(FoodEntity food) {
        if (food == null || food.getCategory() == null) {
            return null;
        }

        return food.getCategory().getId();
    }

    private int safeQuantity(Integer quantity) {
        return quantity != null && quantity > 0 ? quantity : 1;
    }

    private BigDecimal safeRating(FoodEntity food) {
        return food.getRatingAvg() != null ? food.getRatingAvg() : BigDecimal.ZERO;
    }

    private boolean isAvailable(FoodEntity food) {
        if (food == null) {
            return false;
        }

        String status = food.getStatus();

        boolean activeStatus = status == null
                || status.equalsIgnoreCase("AVAILABLE")
                || status.equalsIgnoreCase("ACTIVE");

        boolean hasStock = food.getStock() == null || food.getStock() > 0;

        return activeStatus && hasStock;
    }

    private record ScoredFood(FoodEntity food, double score, String reason) {
    }
}