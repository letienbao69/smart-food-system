package com.food.smart_food_system.Service;

import com.food.smart_food_system.DTO.CategoryDTO;
import com.food.smart_food_system.DTO.CreateCategoryRequest;
import com.food.smart_food_system.DTO.UpdateCategoryRequest;
import com.food.smart_food_system.Entity.CategoryEntity;
import com.food.smart_food_system.Repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public CategoryDTO getCategoryById(Long id) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với id: " + id));

        return mapToDTO(category);
    }

    public CategoryDTO createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new RuntimeException("Tên danh mục đã tồn tại");
        }

        CategoryEntity category = new CategoryEntity();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setStatus("ACTIVE");

        return mapToDTO(categoryRepository.save(category));
    }

    public CategoryDTO updateCategory(Long id, UpdateCategoryRequest request) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với id: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            category.setName(request.getName());
        }

        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            category.setStatus(request.getStatus());
        }

        return mapToDTO(categoryRepository.save(category));
    }

    public void deleteCategory(Long id) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với id: " + id));

        categoryRepository.delete(category);
    }

    private CategoryDTO mapToDTO(CategoryEntity category) {
        return new CategoryDTO(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getStatus()
        );
    }
}