package com.food.smart_food_system.Service;

import com.food.smart_food_system.DTO.CreateFoodRequest;
import com.food.smart_food_system.DTO.FoodDTO;
import com.food.smart_food_system.DTO.UpdateFoodRequest;
import com.food.smart_food_system.Entity.CategoryEntity;
import com.food.smart_food_system.Entity.FoodEntity;
import com.food.smart_food_system.Repository.CategoryRepository;
import com.food.smart_food_system.Repository.FoodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FoodService {

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    public List<FoodDTO> getAllFoods() {
        return foodRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public FoodDTO getFoodById(Long id) {
        FoodEntity food = foodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn với id: " + id));

        return mapToDTO(food);
    }

    public List<FoodDTO> getFoodsByCategory(Long categoryId) {
        return foodRepository.findByCategoryId(categoryId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<FoodDTO> searchFoods(String keyword) {
        return foodRepository.findByNameContainingIgnoreCase(keyword)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public FoodDTO createFood(CreateFoodRequest request) {
        CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với id: " + request.getCategoryId()));

        FoodEntity food = new FoodEntity();
        food.setCategory(category);
        food.setName(request.getName());
        food.setDescription(request.getDescription());
        food.setPrice(request.getPrice());
        food.setStock(request.getStock() != null ? request.getStock() : 0);
        food.setImageUrl(request.getImageUrl());
        food.setStatus("AVAILABLE");
        food.setRatingAvg(BigDecimal.ZERO);

        return mapToDTO(foodRepository.save(food));
    }

    public FoodDTO updateFood(Long id, UpdateFoodRequest request) {
        FoodEntity food = foodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn với id: " + id));

        if (request.getCategoryId() != null) {
            CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với id: " + request.getCategoryId()));
            food.setCategory(category);
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            food.setName(request.getName());
        }

        if (request.getDescription() != null) {
            food.setDescription(request.getDescription());
        }

        if (request.getPrice() != null) {
            food.setPrice(request.getPrice());
        }

        if (request.getStock() != null) {
            food.setStock(request.getStock());
        }

        if (request.getImageUrl() != null) {
            food.setImageUrl(request.getImageUrl());
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            food.setStatus(request.getStatus());
        }

        return mapToDTO(foodRepository.save(food));
    }

    public void deleteFood(Long id) {
        FoodEntity food = foodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn với id: " + id));

        foodRepository.delete(food);
    }

    private FoodDTO mapToDTO(FoodEntity food) {
        return new FoodDTO(
                food.getId(),
                food.getCategory() != null ? food.getCategory().getId() : null,
                food.getCategory() != null ? food.getCategory().getName() : null,
                food.getName(),
                food.getDescription(),
                food.getPrice(),
                food.getStock(),
                food.getImageUrl(),
                food.getStatus(),
                food.getRatingAvg()
        );
    }
}