package com.food.smart_food_system.DTO;

import java.time.LocalDateTime;

public class VerifyResetTokenResponseDTO {

    private Boolean valid;
    private String email;
    private LocalDateTime expiryTime;
    private String message;

    public VerifyResetTokenResponseDTO() {
    }

    public VerifyResetTokenResponseDTO(Boolean valid, String email, LocalDateTime expiryTime, String message) {
        this.valid = valid;
        this.email = email;
        this.expiryTime = expiryTime;
        this.message = message;
    }

    public Boolean getValid() {
        return valid;
    }

    public String getEmail() {
        return email;
    }

    public LocalDateTime getExpiryTime() {
        return expiryTime;
    }

    public String getMessage() {
        return message;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setExpiryTime(LocalDateTime expiryTime) {
        this.expiryTime = expiryTime;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}