package com.food.smart_food_system.Service;

import com.food.smart_food_system.DTO.AdminCreateUserRequest;
import com.food.smart_food_system.DTO.AdminUpdateUserRequest;
import com.food.smart_food_system.DTO.AdminUserResponseDTO;
import com.food.smart_food_system.DTO.UpdateUserStatusRequest;
import com.food.smart_food_system.Entity.RoleEntity;
import com.food.smart_food_system.Entity.UserEntity;
import com.food.smart_food_system.Exception.BusinessException;
import com.food.smart_food_system.Exception.ResourceNotFoundException;
import com.food.smart_food_system.Repository.RoleRepository;
import com.food.smart_food_system.Repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminUserService {

    private static final String ROLE_CUSTOMER = "CUSTOMER";
    private static final Set<String> VALID_STATUS = Set.of("ACTIVE", "BLOCKED");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponseDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminUserResponseDTO getUserById(Long id) {
        return toDto(requireUser(id));
    }

    public AdminUserResponseDTO createUser(AdminCreateUserRequest request) {
        validateCreateRequest(request);

        UserEntity user = new UserEntity();
        user.setFullName(request.getFullName().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPhone(normalizeBlank(request.getPhone()));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAvatarUrl(normalizeBlank(request.getAvatarUrl()));
        user.setStatus(normalizeStatus(request.getStatus(), "ACTIVE"));
        user.setRoles(defaultCustomerRole());

        return toDto(userRepository.save(user));
    }

    public AdminUserResponseDTO updateUser(Long id, AdminUpdateUserRequest request) {
        UserEntity user = requireUser(id);

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String email = request.getEmail().trim().toLowerCase();

            if (userRepository.existsByEmailAndIdNot(email, id)) {
                throw new BusinessException("Email đã tồn tại");
            }

            user.setEmail(email);
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            String phone = request.getPhone().trim();

            if (userRepository.existsByPhoneAndIdNot(phone, id)) {
                throw new BusinessException("Số điện thoại đã tồn tại");
            }

            user.setPhone(phone);
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (request.getPassword().length() < 6) {
                throw new BusinessException("Mật khẩu phải có ít nhất 6 ký tự");
            }

            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(normalizeBlank(request.getAvatarUrl()));
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            user.setStatus(normalizeStatus(request.getStatus(), user.getStatus()));
        }

        return toDto(userRepository.save(user));
    }

    public AdminUserResponseDTO updateStatus(Long id, UpdateUserStatusRequest request) {
        UserEntity user = requireUser(id);

        if (request.getStatus() == null || request.getStatus().isBlank()) {
            throw new BusinessException("Trạng thái không được để trống");
        }

        user.setStatus(normalizeStatus(request.getStatus(), user.getStatus()));

        return toDto(userRepository.save(user));
    }

    public void deleteUser(Long id) {
        UserEntity user = requireUser(id);

        boolean isAdmin = user.getRoles()
                .stream()
                .anyMatch(role -> "ADMIN".equalsIgnoreCase(role.getName()));

        if (isAdmin) {
            throw new BusinessException("Không được xóa tài khoản ADMIN. Hãy khóa tài khoản nếu cần.");
        }

        userRepository.delete(user);
    }

    private void validateCreateRequest(AdminCreateUserRequest request) {
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new BusinessException("Họ tên không được để trống");
        }

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BusinessException("Email không được để trống");
        }

        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Email đã tồn tại");
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            String phone = request.getPhone().trim();

            if (userRepository.existsByPhone(phone)) {
                throw new BusinessException("Số điện thoại đã tồn tại");
            }
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException("Mật khẩu không được để trống");
        }

        if (request.getPassword().length() < 6) {
            throw new BusinessException("Mật khẩu phải có ít nhất 6 ký tự");
        }
    }

    private UserEntity requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    private Set<RoleEntity> defaultCustomerRole() {
        RoleEntity customerRole = roleRepository.findByName(ROLE_CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quyền CUSTOMER"));

        Set<RoleEntity> roles = new HashSet<>();
        roles.add(customerRole);

        return roles;
    }

    private String normalizeStatus(String status, String defaultStatus) {
        if (status == null || status.isBlank()) {
            return defaultStatus;
        }

        String normalized = status.trim().toUpperCase();

        if (!VALID_STATUS.contains(normalized)) {
            throw new BusinessException("Trạng thái user không hợp lệ: " + status);
        }

        return normalized;
    }

    private String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private AdminUserResponseDTO toDto(UserEntity user) {
        AdminUserResponseDTO dto = new AdminUserResponseDTO();

        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setStatus(user.getStatus());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());

        dto.setRoles(user.getRoles()
                .stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toSet()));

        return dto;
    }
}