package com.food.smart_food_system.Controller;

import com.food.smart_food_system.DTO.CreateVoucherRequest;
import com.food.smart_food_system.DTO.UpdateVoucherRequest;
import com.food.smart_food_system.DTO.VoucherDTO;
import com.food.smart_food_system.Reponse.ApiResponse;
import com.food.smart_food_system.Service.VoucherService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
public class VoucherController {
    private final VoucherService service;
    public VoucherController(VoucherService service) { this.service = service; }

    @GetMapping("/validate/{code}") public ResponseEntity<ApiResponse<VoucherDTO>> validate(@PathVariable String code) { return ResponseEntity.ok(ApiResponse.success("Voucher hợp lệ", service.validateCode(code))); }

    @GetMapping("/active") public ResponseEntity<ApiResponse<List<VoucherDTO>>> getActive() { return ResponseEntity.ok(ApiResponse.success("OK", service.getActive())); }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping public ResponseEntity<ApiResponse<List<VoucherDTO>>> getAll() { return ResponseEntity.ok(ApiResponse.success("OK", service.getAll())); }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}") public ResponseEntity<ApiResponse<VoucherDTO>> getById(@PathVariable Long id) { return ResponseEntity.ok(ApiResponse.success("OK", service.getById(id))); }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping public ResponseEntity<ApiResponse<VoucherDTO>> create(@Valid @RequestBody CreateVoucherRequest request) { return ResponseEntity.ok(ApiResponse.success("Tạo voucher thành công", service.create(request))); }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<VoucherDTO>> update(@PathVariable Long id, @Valid @RequestBody UpdateVoucherRequest request) { return ResponseEntity.ok(ApiResponse.success("Cập nhật voucher thành công", service.update(id, request))); }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}") public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.ok(ApiResponse.success("Xóa voucher thành công", null)); }
}
