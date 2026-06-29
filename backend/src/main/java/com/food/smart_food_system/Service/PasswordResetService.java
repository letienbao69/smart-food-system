package com.food.smart_food_system.Service;

import com.food.smart_food_system.DTO.ForgotPasswordRequest;
import com.food.smart_food_system.DTO.ForgotPasswordResponseDTO;
import com.food.smart_food_system.DTO.ResetPasswordRequest;
import com.food.smart_food_system.DTO.VerifyResetTokenResponseDTO;
import com.food.smart_food_system.Entity.PasswordResetTokenEntity;
import com.food.smart_food_system.Entity.UserEntity;
import com.food.smart_food_system.Exception.BusinessException;
import com.food.smart_food_system.Exception.ResourceNotFoundException;
import com.food.smart_food_system.Repository.PasswordResetTokenRepository;
import com.food.smart_food_system.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class PasswordResetService {

    private static final int TOKEN_EXPIRE_MINUTES = 15;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public ForgotPasswordResponseDTO forgotPassword(ForgotPasswordRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BusinessException("Email không được để trống");
        }

        String email = request.getEmail().trim().toLowerCase();

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với email: " + email));

        disableOldTokens(user.getId());

        String token = generateToken();

        PasswordResetTokenEntity resetToken = new PasswordResetTokenEntity();
        resetToken.setUser(user);
        resetToken.setToken(token);
        resetToken.setExpiryTime(LocalDateTime.now().plusMinutes(TOKEN_EXPIRE_MINUTES));
        resetToken.setIsUsed(false);

        passwordResetTokenRepository.save(resetToken);

        // Gửi mã qua email thật nếu đã bật cấu hình mail
        boolean sent = false;
        if (mailEnabled) {
            try {
                emailService.sendResetCode(user.getEmail(), token, TOKEN_EXPIRE_MINUTES);
                sent = true;
            } catch (Exception e) {
                // Không để lộ chi tiết lỗi cho client; ghi log phía server
                System.err.println("Gửi email đặt lại mật khẩu thất bại: " + e.getMessage());
            }
        }

        if (sent) {
            // Khi đã gửi email: KHÔNG trả mã về client (bảo mật)
            return new ForgotPasswordResponseDTO(
                    maskEmail(user.getEmail()),
                    null,
                    resetToken.getExpiryTime(),
                    "Mã xác nhận đã được gửi tới email của bạn. Vui lòng kiểm tra hộp thư (kể cả mục Spam)."
            );
        }

        // Khi chưa bật mail (môi trường dev/test): trả mã trực tiếp để thử nghiệm
        return new ForgotPasswordResponseDTO(
                user.getEmail(),
                token,
                resetToken.getExpiryTime(),
                "Chế độ thử nghiệm: mã được trả trực tiếp. Bật cấu hình email để gửi qua hộp thư người dùng."
        );
    }

    // Che bớt email khi hiển thị, ví dụ: a***@gmail.com
    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return email;
        return email.charAt(0) + "***" + email.substring(at);
    }

    public VerifyResetTokenResponseDTO verifyToken(String token) {
        PasswordResetTokenEntity resetToken = getValidToken(token);

        return new VerifyResetTokenResponseDTO(
                true,
                resetToken.getUser().getEmail(),
                resetToken.getExpiryTime(),
                "Token hợp lệ"
        );
    }

    public void resetPassword(ResetPasswordRequest request) {
        validateResetPasswordRequest(request);

        PasswordResetTokenEntity resetToken = getValidToken(request.getToken());

        UserEntity user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);

        resetToken.setIsUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    private void validateResetPasswordRequest(ResetPasswordRequest request) {
        if (request.getToken() == null || request.getToken().isBlank()) {
            throw new BusinessException("Token không được để trống");
        }

        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new BusinessException("Mật khẩu mới không được để trống");
        }

        if (request.getNewPassword().length() < 6) {
            throw new BusinessException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }

        if (request.getConfirmPassword() == null || request.getConfirmPassword().isBlank()) {
            throw new BusinessException("Xác nhận mật khẩu không được để trống");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("Mật khẩu xác nhận không khớp");
        }
    }

    private PasswordResetTokenEntity getValidToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("Token không được để trống");
        }

        PasswordResetTokenEntity resetToken = passwordResetTokenRepository.findByToken(token.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Token không tồn tại"));

        if (Boolean.TRUE.equals(resetToken.getIsUsed())) {
            throw new BusinessException("Token đã được sử dụng");
        }

        if (resetToken.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Token đã hết hạn");
        }

        return resetToken;
    }

    private void disableOldTokens(Long userId) {
        List<PasswordResetTokenEntity> oldTokens =
                passwordResetTokenRepository.findByUserIdAndIsUsedFalse(userId);

        for (PasswordResetTokenEntity token : oldTokens) {
            token.setIsUsed(true);
        }

        passwordResetTokenRepository.saveAll(oldTokens);
    }

    private String generateToken() {
        // Mã 6 chữ số dễ nhập từ email
        int n = RANDOM.nextInt(1_000_000);
        return String.format("%06d", n);
    }
}