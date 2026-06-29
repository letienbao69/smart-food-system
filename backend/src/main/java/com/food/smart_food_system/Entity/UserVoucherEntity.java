package com.food.smart_food_system.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_vouchers")
public class UserVoucherEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "voucher_id", nullable = false)
    private VoucherEntity voucher;
    @Column(name = "is_used") private Boolean isUsed = false;
    @Column(name = "used_at") private LocalDateTime usedAt;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public UserEntity getUser(){return user;} public void setUser(UserEntity user){this.user=user;}
    public VoucherEntity getVoucher(){return voucher;} public void setVoucher(VoucherEntity voucher){this.voucher=voucher;}
    public Boolean getIsUsed(){return isUsed;} public void setIsUsed(Boolean used){isUsed=used;}
    public LocalDateTime getUsedAt(){return usedAt;} public void setUsedAt(LocalDateTime usedAt){this.usedAt=usedAt;}
}
