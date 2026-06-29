package com.food.smart_food_system.DTO;

import java.time.LocalDateTime;

public class ForgotPasswordResponseDTO {

    private String email;
    private String resetToken;
    private LocalDateTime expiryTime;
    private String note;

    public ForgotPasswordResponseDTO() {
    }

    public ForgotPasswordResponseDTO(String email, String resetToken, LocalDateTime expiryTime, String note) {
        this.email = email;
        this.resetToken = resetToken;
        this.expiryTime = expiryTime;
        this.note = note;
    }

    public String getEmail() {
        return email;
    }

    public String getResetToken() {
        return resetToken;
    }

    public LocalDateTime getExpiryTime() {
        return expiryTime;
    }

    public String getNote() {
        return note;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public void setExpiryTime(LocalDateTime expiryTime) {
        this.expiryTime = expiryTime;
    }

    public void setNote(String note) {
        this.note = note;
    }
}