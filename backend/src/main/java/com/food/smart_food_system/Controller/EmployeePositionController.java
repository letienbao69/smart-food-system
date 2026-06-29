package com.food.smart_food_system.Controller;

import com.food.smart_food_system.DTO.CreateEmployeePositionRequest;
import com.food.smart_food_system.DTO.EmployeePositionDTO;
import com.food.smart_food_system.DTO.UpdateEmployeePositionRequest;
import com.food.smart_food_system.Reponse.ApiResponse;
import com.food.smart_food_system.Service.EmployeePositionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/employee-positions")
@PreAuthorize("hasRole('ADMIN')")
public class EmployeePositionController {
    private final EmployeePositionService service;
    public EmployeePositionController(EmployeePositionService service) { this.service = service; }

    @GetMapping public ResponseEntity<ApiResponse<List<EmployeePositionDTO>>> getAll() { return ResponseEntity.ok(ApiResponse.success("OK", service.getAll())); }
    @GetMapping("/{id}") public ResponseEntity<ApiResponse<EmployeePositionDTO>> getById(@PathVariable Long id) { return ResponseEntity.ok(ApiResponse.success("OK", service.getById(id))); }
    @PostMapping public ResponseEntity<ApiResponse<EmployeePositionDTO>> create(@Valid @RequestBody CreateEmployeePositionRequest request) { return ResponseEntity.ok(ApiResponse.success("Tạo chức vụ thành công", service.create(request))); }
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<EmployeePositionDTO>> update(@PathVariable Long id, @Valid @RequestBody UpdateEmployeePositionRequest request) { return ResponseEntity.ok(ApiResponse.success("Cập nhật chức vụ thành công", service.update(id, request))); }
    @DeleteMapping("/{id}") public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.ok(ApiResponse.success("Xóa chức vụ thành công", null)); }
}
