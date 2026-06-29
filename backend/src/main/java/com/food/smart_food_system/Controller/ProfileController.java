package com.food.smart_food_system.Controller;

import com.food.smart_food_system.Entity.UserEntity;
import com.food.smart_food_system.Exception.BusinessException;
import com.food.smart_food_system.Reponse.ApiResponse;
import com.food.smart_food_system.Repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Hồ sơ người dùng hiện tại: xem / cập nhật thông tin cá nhân + avatar,
 * và đổi mật khẩu. Phục vụ trang "Cài đặt tài khoản".
 */
@RestController
@RequestMapping("/api/users")
public class ProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(Authentication auth) {
        return ApiResponse.success("OK", toMap(currentUser(auth)));
    }

    @PutMapping("/me")
    public ApiResponse<Map<String, Object>> updateMe(Authentication auth, @RequestBody Map<String, Object> body) {
        UserEntity user = currentUser(auth);
        if (body.containsKey("fullName")) user.setFullName(str(body.get("fullName")));
        if (body.containsKey("phone")) user.setPhone(str(body.get("phone")));
        if (body.containsKey("address")) user.setAddress(str(body.get("address")));
        if (body.containsKey("avatarUrl")) user.setAvatarUrl(str(body.get("avatarUrl")));
        if (body.containsKey("dateOfBirth")) {
            String d = str(body.get("dateOfBirth"));
            user.setDateOfBirth(d == null || d.isBlank() ? null : LocalDate.parse(d.substring(0, 10)));
        }
        userRepository.save(user);
        return ApiResponse.success("Thông tin cá nhân đã được cập nhật thành công!", toMap(user));
    }

    @PutMapping("/me/password")
    public ApiResponse<Object> changePassword(Authentication auth, @RequestBody Map<String, String> body) {
        UserEntity user = currentUser(auth);
        String oldPass = body.get("oldPassword");
        String newPass = body.get("newPassword");
        if (newPass == null || newPass.length() < 6) {
            throw new BusinessException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }
        if (oldPass == null || !passwordEncoder.matches(oldPass, user.getPassword())) {
            throw new BusinessException("Mật khẩu hiện tại không đúng");
        }
        user.setPassword(passwordEncoder.encode(newPass));
        userRepository.save(user);
        return ApiResponse.success("Đổi mật khẩu thành công", null);
    }

    private UserEntity currentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng"));
    }

    private Map<String, Object> toMap(UserEntity u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("fullName", u.getFullName());
        m.put("email", u.getEmail());
        m.put("phone", u.getPhone());
        m.put("address", u.getAddress());
        m.put("avatarUrl", u.getAvatarUrl());
        m.put("dateOfBirth", u.getDateOfBirth());
        return m;
    }

    private String str(Object o) { return o == null ? null : String.valueOf(o); }
}
