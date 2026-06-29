package com.food.smart_food_system.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Gửi email cho người dùng (dùng cho luồng quên mật khẩu).
 * Cấu hình SMTP trong application.properties (spring.mail.*).
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Gửi mã OTP đặt lại mật khẩu tới email người dùng.
     */
    public void sendResetCode(String toEmail, String code, int expireMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (fromEmail != null && !fromEmail.isBlank()) {
            message.setFrom(fromEmail);
        }
        message.setTo(toEmail);
        message.setSubject("SmartFood - Mã đặt lại mật khẩu");
        message.setText(
                "Xin chào,\n\n" +
                "Bạn vừa yêu cầu đặt lại mật khẩu cho tài khoản SmartFood.\n\n" +
                "Mã xác nhận của bạn là: " + code + "\n\n" +
                "Mã có hiệu lực trong " + expireMinutes + " phút. " +
                "Vui lòng không chia sẻ mã này với bất kỳ ai.\n\n" +
                "Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.\n\n" +
                "Trân trọng,\nĐội ngũ SmartFood"
        );
        mailSender.send(message);
    }
}
