package com.food.smart_food_system.Controller;

import com.food.smart_food_system.DTO.CreateTableRequest;
import com.food.smart_food_system.DTO.TableDTO;
import com.food.smart_food_system.DTO.UpdateTableRequest;
import com.food.smart_food_system.Reponse.ApiResponse;
import com.food.smart_food_system.Service.TableService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tables")
public class TableController {

    private final TableService service;

    public TableController(TableService service) {
        this.service = service;
    }

    // Công khai: danh sách bàn để hiển thị sơ đồ + chọn khi đặt
    @GetMapping
    public ResponseEntity<ApiResponse<List<TableDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getAll()));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<TableDTO>>> getAvailable(
            @RequestParam(required = false) Integer minCapacity) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getAvailable(minCapacity)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TableDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getById(id)));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PostMapping
    public ResponseEntity<ApiResponse<TableDTO>> create(@Valid @RequestBody CreateTableRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tạo bàn thành công", service.create(request)));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TableDTO>> update(@PathVariable Long id,
                                                        @Valid @RequestBody UpdateTableRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật bàn thành công", service.update(id, request)));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa bàn", null));
    }
}
