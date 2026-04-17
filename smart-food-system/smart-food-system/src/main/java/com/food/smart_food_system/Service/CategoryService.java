package com.food.smart_food_system.Service;

import com.food.smart_food_system.DTO.CategoryDTO;
import com.food.smart_food_system.DTO.CreateCategoryRequest;
import com.food.smart_food_system.DTO.UpdateCategoryRequest;
import com.food.smart_food_system.Entity.CategoryEntity;

import java.util.List;

public interface CategoryService {
    List<CategoryDTO> getAllCategories();
    CategoryDTO getCategoryById(Long id);
    CategoryDTO createCategory(CreateCategoryRequest request);
    CategoryDTO updateCategory(Long id, UpdateCategoryRequest request);
    void deleteCategory(Long id);
    void deleteAll(List<Long> ids);
    CategoryDTO mapToDTO(CategoryEntity category);

}
