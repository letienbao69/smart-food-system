package com.food.smart_food_system.Controller;

import com.food.smart_food_system.DTO.CreateFoodRequest;
import com.food.smart_food_system.DTO.FoodDTO;
import com.food.smart_food_system.DTO.UpdateFoodRequest;
import com.food.smart_food_system.Reponse.ApiResponse;
import com.food.smart_food_system.Service.FoodService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/foods")
public class FoodController {
    private final FoodService service;
    public FoodController(FoodService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FoodDTO>>> getAll(@RequestParam(required = false) Long categoryId,
                                                             @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getAll(categoryId, keyword)));
    }

    @GetMapping("/featured") public ResponseEntity<ApiResponse<List<FoodDTO>>> featured() { return ResponseEntity.ok(ApiResponse.success("OK", service.getFeatured())); }

    @GetMapping("/{id}") public ResponseEntity<ApiResponse<FoodDTO>> getById(@PathVariable Long id) { return ResponseEntity.ok(ApiResponse.success("OK", service.getById(id))); }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping public ResponseEntity<ApiResponse<FoodDTO>> create(@Valid @RequestBody CreateFoodRequest request) { return ResponseEntity.ok(ApiResponse.success("Tạo món ăn thành công", service.create(request))); }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<FoodDTO>> update(@PathVariable Long id, @Valid @RequestBody UpdateFoodRequest request) { return ResponseEntity.ok(ApiResponse.success("Cập nhật món ăn thành công", service.update(id, request))); }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}") public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.ok(ApiResponse.success("Xóa món ăn thành công", null)); }
}
