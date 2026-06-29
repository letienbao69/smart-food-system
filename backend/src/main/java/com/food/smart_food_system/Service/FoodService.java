package com.food.smart_food_system.Service;

import com.food.smart_food_system.DTO.CreateFoodRequest;
import com.food.smart_food_system.DTO.FoodDTO;
import com.food.smart_food_system.DTO.UpdateFoodRequest;
import java.util.List;

public interface FoodService {
    List<FoodDTO> getAll(Long categoryId, String keyword);
    FoodDTO getById(Long id);
    FoodDTO create(CreateFoodRequest request);
    FoodDTO update(Long id, UpdateFoodRequest request);
    void delete(Long id);
    List<FoodDTO> getFeatured();
}
