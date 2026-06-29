package com.food.smart_food_system.Service;

import com.food.smart_food_system.DTO.RoleRequest;
import com.food.smart_food_system.DTO.RoleResponseDTO;
import com.food.smart_food_system.DTO.UpdateUserRolesRequest;
import com.food.smart_food_system.DTO.UserRoleResponseDTO;
import com.food.smart_food_system.Entity.RoleEntity;
import com.food.smart_food_system.Entity.UserEntity;
import com.food.smart_food_system.Exception.BusinessException;
import com.food.smart_food_system.Exception.ResourceNotFoundException;
import com.food.smart_food_system.Repository.RoleRepository;
import com.food.smart_food_system.Repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class PermissionService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public PermissionService(
            RoleRepository roleRepository,
            UserRepository userRepository
    ) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleResponseDTO> getAllRoles() {
        return roleRepository.findAll()
                .stream()
                .map(this::toRoleDto)
                .toList();
    }

    public RoleResponseDTO createRole(RoleRequest request) {
        String roleName = normalizeRoleName(request.getName());

        if (roleRepository.existsByName(roleName)) {
            throw new BusinessException("Quyền đã tồn tại: " + roleName);
        }

        RoleEntity role = new RoleEntity();
        role.setName(roleName);

        return toRoleDto(roleRepository.save(role));
    }

    public RoleResponseDTO updateRole(Long roleId, RoleRequest request) {
        RoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quyền"));

        String newRoleName = normalizeRoleName(request.getName());

        if (!role.getName().equalsIgnoreCase(newRoleName) && roleRepository.existsByName(newRoleName)) {
            throw new BusinessException("Tên quyền đã tồn tại: " + newRoleName);
        }

        if (isSystemRole(role.getName()) && !role.getName().equalsIgnoreCase(newRoleName)) {
            throw new BusinessException("Không được đổi tên quyền mặc định của hệ thống");
        }

        role.setName(newRoleName);

        return toRoleDto(roleRepository.save(role));
    }

    public void deleteRole(Long roleId) {
        RoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quyền"));

        if (isSystemRole(role.getName())) {
            throw new BusinessException("Không được xóa quyền mặc định của hệ thống");
        }

        if (userRepository.existsByRoles_Id(roleId)) {
            throw new BusinessException("Không thể xóa quyền vì đang có người dùng sử dụng quyền này");
        }

        roleRepository.delete(role);
    }

    @Transactional(readOnly = true)
    public UserRoleResponseDTO getUserRoles(Long userId) {
        UserEntity user = requireUser(userId);
        return toUserRoleDto(user);
    }

    public UserRoleResponseDTO updateUserRoles(Long userId, UpdateUserRolesRequest request) {
        UserEntity user = requireUser(userId);

        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            throw new BusinessException("Danh sách quyền không được để trống");
        }

        Set<RoleEntity> roles = resolveRoles(request.getRoles());

        user.setRoles(roles);

        return toUserRoleDto(userRepository.save(user));
    }

    private UserEntity requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    private Set<RoleEntity> resolveRoles(Set<String> roleNames) {
        Set<RoleEntity> roles = new HashSet<>();

        for (String roleNameRaw : roleNames) {
            String roleName = normalizeRoleName(roleNameRaw);

            RoleEntity role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quyền: " + roleName));

            roles.add(role);
        }

        return roles;
    }

    private String normalizeRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            throw new BusinessException("Tên quyền không được để trống");
        }

        String normalized = roleName.trim().toUpperCase();

        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }

        return normalized;
    }

    private boolean isSystemRole(String roleName) {
        return "ADMIN".equalsIgnoreCase(roleName)
                || "CUSTOMER".equalsIgnoreCase(roleName);
    }

    private RoleResponseDTO toRoleDto(RoleEntity role) {
        return new RoleResponseDTO(role.getId(), role.getName());
    }

    private UserRoleResponseDTO toUserRoleDto(UserEntity user) {
        UserRoleResponseDTO dto = new UserRoleResponseDTO();

        dto.setUserId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setStatus(user.getStatus());

        dto.setRoles(user.getRoles()
                .stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toSet()));

        return dto;
    }
}