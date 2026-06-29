package com.food.smart_food_system.DTO;

import java.util.Set;

public class UserRoleResponseDTO {

    private Long userId;
    private String fullName;
    private String email;
    private String status;
    private Set<String> roles;

    public UserRoleResponseDTO() {
    }

    public Long getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getStatus() {
        return status;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}