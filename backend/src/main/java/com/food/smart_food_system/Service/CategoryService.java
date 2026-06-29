package com.food.smart_food_system.Service;

import com.food.smart_food_system.DTO.CategoryDTO;
import com.food.smart_food_system.DTO.CreateCategoryRequest;
import com.food.smart_food_system.DTO.UpdateCategoryRequest;
import java.util.List;

public interface CategoryService {
    List<CategoryDTO> getAll();
    CategoryDTO getById(Long id);
    CategoryDTO create(CreateCategoryRequest request);
    CategoryDTO update(Long id, UpdateCategoryRequest request);
    void delete(Long id);
}
