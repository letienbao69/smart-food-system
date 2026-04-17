package com.food.smart_food_system.Service.impl;

import com.food.smart_food_system.DTO.CategoryDTO;
import com.food.smart_food_system.DTO.CreateCategoryRequest;
import com.food.smart_food_system.DTO.UpdateCategoryRequest;
import com.food.smart_food_system.Entity.CategoryEntity;
import com.food.smart_food_system.Repository.CategoryRepository;
import com.food.smart_food_system.Service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    @Override
    public CategoryDTO getCategoryById(Long id) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với id: " + id));

        return mapToDTO(category);
    }
    @Override
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
    @Override
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
    @Override
    public void deleteCategory(Long id) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với id: " + id));

        categoryRepository.delete(category);
    }

    @Override
    public void deleteAll(List<Long> ids) {
        try {
            List<CategoryEntity> buildingEntities = categoryRepository.findByIdIn(ids);

            categoryRepository.deleteAll(buildingEntities);
        } catch (Exception e) {
            throw new RuntimeException("Xoá Lỗi rồi", e);
        }
    }

    @Override
    public CategoryDTO mapToDTO(CategoryEntity category) {
        return new CategoryDTO(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getStatus()
        );
    }
}