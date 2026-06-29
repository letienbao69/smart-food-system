package com.food.smart_food_system.Controller;

import com.food.smart_food_system.DTO.AdminCreateUserRequest;
import com.food.smart_food_system.DTO.AdminUpdateUserRequest;
import com.food.smart_food_system.DTO.UpdateUserStatusRequest;
import com.food.smart_food_system.Reponse.ApiResponse;
import com.food.smart_food_system.Service.AdminUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách người dùng thành công",
                adminUserService.getAllUsers()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy chi tiết người dùng thành công",
                adminUserService.getUserById(id)
        ));
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody AdminCreateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tạo người dùng thành công",
                adminUserService.createUser(request)
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestBody AdminUpdateUserRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật người dùng thành công",
                adminUserService.updateUser(id, request)
        ));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateUserStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật trạng thái người dùng thành công",
                adminUserService.updateStatus(id, request)
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        adminUserService.deleteUser(id);

        return ResponseEntity.ok(ApiResponse.success(
                "Xóa người dùng thành công",
                null
        ));
    }
}