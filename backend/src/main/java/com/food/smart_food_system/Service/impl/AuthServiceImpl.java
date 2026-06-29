package com.food.smart_food_system.Service.impl;

import com.food.smart_food_system.Config.JwtTokenProvider;
import com.food.smart_food_system.DTO.LoginRequest;
import com.food.smart_food_system.DTO.RegisterRequest;
import com.food.smart_food_system.Entity.RoleEntity;
import com.food.smart_food_system.Entity.UserEntity;
import com.food.smart_food_system.Exception.BusinessException;
import com.food.smart_food_system.Reponse.AuthResponse;
import com.food.smart_food_system.Repository.RoleRepository;
import com.food.smart_food_system.Repository.UserRepository;
import com.food.smart_food_system.Service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider,
                           UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        String token = jwtTokenProvider.generateToken(authentication);
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng"));
        java.util.List<String> roleNames = user.getRoles().stream()
                .map(RoleEntity::getName).distinct().collect(java.util.stream.Collectors.toList());
        String role = primaryRole(roleNames);
        return new AuthResponse(token, user.getEmail(), user.getFullName(), role, user.getPhone(), user.getAvatarUrl(), roleNames);
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email đã tồn tại");
        }
        if (request.getPhone() != null && !request.getPhone().isBlank() && userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("Số điện thoại đã tồn tại");
        }

        RoleEntity customerRole = roleRepository.findByName("CUSTOMER")
                .orElseGet(() -> {
                    RoleEntity role = new RoleEntity();
                    role.setName("CUSTOMER");
                    return roleRepository.save(role);
                });

        UserEntity user = new UserEntity();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus("ACTIVE");
        user.setRoles(Set.of(customerRole));
        userRepository.save(user);

        return login(toLogin(request));
    }

    @Override
    public AuthResponse me(String email) {
        UserEntity user = userRepository.findByEmail(email).orElseThrow(() -> new BusinessException("Không tìm thấy người dùng"));
        java.util.List<String> roleNames = user.getRoles().stream()
                .map(RoleEntity::getName).distinct().collect(java.util.stream.Collectors.toList());
        String role = primaryRole(roleNames);
        return new AuthResponse(null, user.getEmail(), user.getFullName(), role, user.getPhone(), user.getAvatarUrl(), roleNames);
    }

    // Chọn vai trò chính theo thứ tự ưu tiên: ADMIN > STAFF > CUSTOMER
    private String primaryRole(java.util.List<String> roleNames) {
        if (roleNames.stream().anyMatch(r -> r.equalsIgnoreCase("ADMIN"))) return "ADMIN";
        if (roleNames.stream().anyMatch(r -> r.equalsIgnoreCase("STAFF"))) return "STAFF";
        return roleNames.isEmpty() ? "CUSTOMER" : roleNames.get(0);
    }

    private LoginRequest toLogin(RegisterRequest request) {
        LoginRequest login = new LoginRequest();
        login.setEmail(request.getEmail());
        login.setPassword(request.getPassword());
        return login;
    }
}
