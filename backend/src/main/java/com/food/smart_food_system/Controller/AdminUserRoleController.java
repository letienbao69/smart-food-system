package com.food.smart_food_system.Controller;

import com.food.smart_food_system.DTO.UpdateUserRolesRequest;
import com.food.smart_food_system.Reponse.ApiResponse;
import com.food.smart_food_system.Service.PermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserRoleController {

    private final PermissionService permissionService;

    public AdminUserRoleController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping("/{userId}/roles")
    public ResponseEntity<?> getUserRoles(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy quyền của người dùng thành công",
                permissionService.getUserRoles(userId)
        ));
    }

    @PutMapping("/{userId}/roles")
    public ResponseEntity<?> updateUserRoles(
            @PathVariable Long userId,
            @RequestBody UpdateUserRolesRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật quyền người dùng thành công",
                permissionService.updateUserRoles(userId, request)
        ));
    }
}