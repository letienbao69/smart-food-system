package com.food.smart_food_system.Controller;

import com.food.smart_food_system.DTO.CategoryDTO;
import com.food.smart_food_system.DTO.CreateCategoryRequest;
import com.food.smart_food_system.DTO.UpdateCategoryRequest;
import com.food.smart_food_system.DTO.chatbot.ChatRequest;
import com.food.smart_food_system.DTO.chatbot.ChatResponse;
import com.food.smart_food_system.Reponse.ApiResponse;
import com.food.smart_food_system.Service.CategoryService;
import com.food.smart_food_system.Service.impl.ChatbotServiceimpl;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService service;
    private final ChatbotServiceimpl chatbotServiceimpl;

    public CategoryController(CategoryService service, ChatbotServiceimpl chatbotServiceimpl) { this.service = service;
        this.chatbotServiceimpl = chatbotServiceimpl;
    }

    @GetMapping public ResponseEntity<ApiResponse<List<CategoryDTO>>> getAll() { return ResponseEntity.ok(ApiResponse.success("OK", service.getAll())); }
    @GetMapping("/{id}") public ResponseEntity<ApiResponse<CategoryDTO>> getById(@PathVariable Long id) { return ResponseEntity.ok(ApiResponse.success("OK", service.getById(id))); }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping public ResponseEntity<ApiResponse<CategoryDTO>> create(@Valid @RequestBody CreateCategoryRequest request) { return ResponseEntity.ok(ApiResponse.success("Tạo danh mục thành công", service.create(request))); }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<CategoryDTO>> update(@PathVariable Long id, @Valid @RequestBody UpdateCategoryRequest request) { return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục thành công", service.update(id, request))); }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}") public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.ok(ApiResponse.success("Xóa danh mục thành công", null)); }


}
