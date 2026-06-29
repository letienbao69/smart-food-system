package com.food.smart_food_system.Controller;

import com.food.smart_food_system.DTO.CreateEmployeeRequest;
import com.food.smart_food_system.DTO.EmployeeDTO;
import com.food.smart_food_system.DTO.UpdateEmployeeRequest;
import com.food.smart_food_system.Reponse.ApiResponse;
import com.food.smart_food_system.Service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
@PreAuthorize("hasRole('ADMIN')")
public class EmployeeController {
    private final EmployeeService service;
    public EmployeeController(EmployeeService service) { this.service = service; }

    @GetMapping public ResponseEntity<ApiResponse<List<EmployeeDTO>>> getAll() { return ResponseEntity.ok(ApiResponse.success("OK", service.getAll())); }
    @GetMapping("/{id}") public ResponseEntity<ApiResponse<EmployeeDTO>> getById(@PathVariable Long id) { return ResponseEntity.ok(ApiResponse.success("OK", service.getById(id))); }
    @PostMapping public ResponseEntity<ApiResponse<EmployeeDTO>> create(@Valid @RequestBody CreateEmployeeRequest request) { return ResponseEntity.ok(ApiResponse.success("Tạo nhân viên thành công", service.create(request))); }
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<EmployeeDTO>> update(@PathVariable Long id, @Valid @RequestBody UpdateEmployeeRequest request) { return ResponseEntity.ok(ApiResponse.success("Cập nhật nhân viên thành công", service.update(id, request))); }
    @DeleteMapping("/{id}") public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.ok(ApiResponse.success("Xóa nhân viên thành công", null)); }
}
