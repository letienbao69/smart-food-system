package com.food.smart_food_system.Controller;

import com.food.smart_food_system.DTO.RoleRequest;
import com.food.smart_food_system.Reponse.ApiResponse;
import com.food.smart_food_system.Service.PermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/roles")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoleController {

    private final PermissionService permissionService;

    public AdminRoleController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    public ResponseEntity<?> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách quyền thành công",
                permissionService.getAllRoles()
        ));
    }

    @PostMapping
    public ResponseEntity<?> createRole(@RequestBody RoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tạo quyền thành công",
                permissionService.createRole(request)
        ));
    }

    @PutMapping("/{roleId}")
    public ResponseEntity<?> updateRole(
            @PathVariable Long roleId,
            @RequestBody RoleRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật quyền thành công",
                permissionService.updateRole(roleId, request)
        ));
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<?> deleteRole(@PathVariable Long roleId) {
        permissionService.deleteRole(roleId);

        return ResponseEntity.ok(ApiResponse.success(
                "Xóa quyền thành công",
                null
        ));
    }
}