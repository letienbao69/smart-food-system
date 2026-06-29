package com.food.smart_food_system.Service.impl;

import com.food.smart_food_system.DTO.CreateFoodRequest;
import com.food.smart_food_system.DTO.FoodDTO;
import com.food.smart_food_system.DTO.UpdateFoodRequest;
import com.food.smart_food_system.Entity.CategoryEntity;
import com.food.smart_food_system.Entity.FoodEntity;
import com.food.smart_food_system.Exception.ResourceNotFoundException;
import com.food.smart_food_system.Repository.CategoryRepository;
import com.food.smart_food_system.Repository.FoodRepository;
import com.food.smart_food_system.Service.FoodService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class FoodServiceImpl implements FoodService {

    private final FoodRepository foodRepository;
    private final CategoryRepository categoryRepository;

    public FoodServiceImpl(FoodRepository foodRepository, CategoryRepository categoryRepository) {
        this.foodRepository = foodRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<FoodDTO> getAll(Long categoryId, String keyword) {
        List<FoodEntity> foods;
        if (categoryId != null) {
            foods = foodRepository.findByCategoryId(categoryId);
        } else if (keyword != null && !keyword.isBlank()) {
            foods = foodRepository.findByNameContainingIgnoreCase(keyword);
        } else {
            foods = foodRepository.findAll();
        }
        return foods.stream().map(this::toDto).toList();
    }

    @Override
    public FoodDTO getById(Long id) {
        return toDto(find(id));
    }

    @Override
    public FoodDTO create(CreateFoodRequest request) {
        CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));
        FoodEntity entity = new FoodEntity();
        apply(entity, request, category);
        return toDto(foodRepository.save(entity));
    }

    @Override
    public FoodDTO update(Long id, UpdateFoodRequest request) {
        FoodEntity entity = find(id);
        CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));
        apply(entity, request, category);
        return toDto(foodRepository.save(entity));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void delete(Long id) {
        FoodEntity food = find(id);
        // Attempt hard delete first. If there are order_items referencing this
        // food we fall back to a soft-delete (HIDDEN status) so that order
        // history stays intact — same pattern as any real food ordering platform.
        try {
            foodRepository.delete(food);
            foodRepository.flush();
        } catch (Exception e) {
            // FK constraint from order_items — soft delete instead
            food.setStatus("HIDDEN");
            food.setName("[ĐÃ XOÁ] " + food.getName().replace("[ĐÃ XOÁ] ", ""));
            foodRepository.save(food);
        }
    }

    @Override
    public List<FoodDTO> getFeatured() {
        return foodRepository.findFeatured().stream().map(this::toDto).toList();
    }

    private void apply(FoodEntity entity, CreateFoodRequest request, CategoryEntity category) {
        entity.setCategory(category);
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setPrice(request.getPrice());
        entity.setStock(request.getStock() == null ? 0 : request.getStock());
        entity.setImageUrl(request.getImageUrl());
        entity.setStatus(request.getStatus() == null || request.getStatus().isBlank()
                ? "AVAILABLE"
                : request.getStatus());
        entity.setCalories(request.getCalories());
        entity.setProteinG(request.getProteinG());
        entity.setFatG(request.getFatG());
        entity.setCarbsG(request.getCarbsG());
        entity.setTags(request.getTags());
        entity.setDiscountPercent(request.getDiscountPercent() == null ? 0 : request.getDiscountPercent());
        entity.setPrepTimeMinutes(request.getPrepTimeMinutes());
        entity.setIngredients(request.getIngredients());
        if (entity.getRatingAvg() == null) {
            entity.setRatingAvg(BigDecimal.ZERO);
        }
    }

    private FoodEntity find(Long id) {
        return foodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy món ăn"));
    }

    private FoodDTO toDto(FoodEntity e) {
        FoodDTO dto = new FoodDTO();
        dto.setId(e.getId());
        if (e.getCategory() != null) {
            dto.setCategoryId(e.getCategory().getId());
            dto.setCategoryName(e.getCategory().getName());
        }
        dto.setName(e.getName());
        dto.setDescription(e.getDescription());
        dto.setPrice(e.getPrice());
        dto.setStock(e.getStock());
        dto.setImageUrl(e.getImageUrl());
        dto.setStatus(e.getStatus());
        dto.setRatingAvg(e.getRatingAvg());
        dto.setCalories(e.getCalories());
        dto.setProteinG(e.getProteinG());
        dto.setFatG(e.getFatG());
        dto.setCarbsG(e.getCarbsG());
        dto.setTags(e.getTags());
        dto.setDiscountPercent(e.getDiscountPercent());
        dto.setPrepTimeMinutes(e.getPrepTimeMinutes());
        dto.setIngredients(e.getIngredients());
        return dto;
    }
}
