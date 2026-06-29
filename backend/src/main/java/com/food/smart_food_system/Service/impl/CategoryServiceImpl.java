package com.food.smart_food_system.Service.impl;

import com.food.smart_food_system.DTO.CategoryDTO;
import com.food.smart_food_system.DTO.CreateCategoryRequest;
import com.food.smart_food_system.DTO.UpdateCategoryRequest;
import com.food.smart_food_system.Entity.CategoryEntity;
import com.food.smart_food_system.Exception.BusinessException;
import com.food.smart_food_system.Exception.ResourceNotFoundException;
import com.food.smart_food_system.Repository.CategoryRepository;
import com.food.smart_food_system.Service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    public CategoryServiceImpl(CategoryRepository categoryRepository) { this.categoryRepository = categoryRepository; }

    @Override
    public List<CategoryDTO> getAll() { return categoryRepository.findAll().stream().map(this::toDto).toList(); }

    @Override
    public CategoryDTO getById(Long id) { return toDto(find(id)); }

    @Override
    public CategoryDTO create(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) throw new BusinessException("Tên danh mục đã tồn tại");
        CategoryEntity entity = new CategoryEntity();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setStatus(request.getStatus() == null || request.getStatus().isBlank() ? "ACTIVE" : request.getStatus());
        return toDto(categoryRepository.save(entity));
    }

    @Override
    public CategoryDTO update(Long id, UpdateCategoryRequest request) {
        CategoryEntity entity = find(id);
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        if (request.getStatus() != null) entity.setStatus(request.getStatus());
        if (request.getFeatured() != null) entity.setFeatured(request.getFeatured());
        return toDto(categoryRepository.save(entity));
    }

    @Override
    public void delete(Long id) { categoryRepository.delete(find(id)); }

    private CategoryEntity find(Long id) { return categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục")); }
    private CategoryDTO toDto(CategoryEntity e) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(e.getId()); dto.setName(e.getName()); dto.setDescription(e.getDescription()); dto.setStatus(e.getStatus()); dto.setFeatured(e.getFeatured());
        return dto;
    }
}
