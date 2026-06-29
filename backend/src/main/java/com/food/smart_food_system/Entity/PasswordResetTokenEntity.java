package com.food.smart_food_system.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetTokenEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private UserEntity user;
    @Column(nullable = false, unique = true) private String token;
    @Column(name = "expiry_time", nullable = false) private LocalDateTime expiryTime;
    @Column(name = "is_used") private Boolean isUsed = false;
    @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public UserEntity getUser(){return user;} public void setUser(UserEntity user){this.user=user;}
    public String getToken(){return token;} public void setToken(String token){this.token=token;}
    public LocalDateTime getExpiryTime(){return expiryTime;} public void setExpiryTime(LocalDateTime expiryTime){this.expiryTime=expiryTime;}
    public Boolean getIsUsed(){return isUsed;} public void setIsUsed(Boolean used){isUsed=used;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime createdAt){this.createdAt=createdAt;}
}
