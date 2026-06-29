package com.food.smart_food_system.DTO;

import java.time.LocalDateTime;

public class NotificationResponseDTO {

    private Long id;
    private String type;
    private String title;
    private String message;
    private String targetRole;
    private Long targetUserId;
    private String targetUserName;
    private String referenceType;
    private Long referenceId;
    private Boolean readStatus;
    private LocalDateTime createdAt;

    public NotificationResponseDTO() {
    }

    public NotificationResponseDTO(
            Long id,
            String type,
            String title,
            String message,
            String targetRole,
            String referenceType,
            Long referenceId,
            Boolean readStatus,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.message = message;
        this.targetRole = targetRole;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.readStatus = readStatus;
        this.createdAt = createdAt;
    }

    public NotificationResponseDTO(
            Long id,
            String type,
            String title,
            String message,
            String targetRole,
            Long targetUserId,
            String targetUserName,
            String referenceType,
            Long referenceId,
            Boolean readStatus,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.message = message;
        this.targetRole = targetRole;
        this.targetUserId = targetUserId;
        this.targetUserName = targetUserName;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.readStatus = readStatus;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public String getTargetUserName() {
        return targetUserName;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public Boolean getReadStatus() {
        return readStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    public void setTargetUserName(String targetUserName) {
        this.targetUserName = targetUserName;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public void setReadStatus(Boolean readStatus) {
        this.readStatus = readStatus;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}