package com.food.smart_food_system.Entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Đặt bàn — entity trung tâm của hệ thống ăn tại nhà hàng.
 * Vòng đời trạng thái:
 *   PENDING (chờ xác nhận) -> CONFIRMED (đã xác nhận) -> SEATED (đã nhận bàn)
 *   -> COMPLETED (hoàn tất) | CANCELLED (đã hủy) | NO_SHOW (khách không đến)
 *
 * Một đặt bàn CÓ THỂ kèm 1 đơn món đặt trước (OrderEntity.reservation),
 * hoặc chỉ giữ bàn rồi gọi món tại quán (linh hoạt).
 */
@Entity
@Table(name = "reservations")
public class ReservationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private UserEntity user;
    // Bàn có thể chưa gán lúc khách đặt; admin xác nhận và gán bàn cụ thể.
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "table_id") private RestaurantTableEntity table;
    @Column(name = "reservation_code", nullable = false, unique = true) private String reservationCode;
    @Column(name = "guest_name", nullable = false) private String guestName;
    @Column(name = "guest_phone", nullable = false) private String guestPhone;
    @Column(name = "party_size", nullable = false) private Integer partySize;
    @Column(name = "reservation_time", nullable = false) private LocalDateTime reservationTime;
    @Column(nullable = false) private String status = "PENDING";
    // Đặt cọc giữ bàn qua chuyển khoản QR ở nhà
    @Column(name = "deposit_amount") private BigDecimal depositAmount = BigDecimal.ZERO;
    @Column(name = "deposit_status") private String depositStatus = "NONE"; // NONE | PENDING | PAID
    // Hình thức thanh toán phần còn lại TẠI QUÁN: CASH | BANK_TRANSFER
    @Column(name = "payment_method") private String paymentMethod = "CASH";
    @Column(name = "has_preorder") private Boolean hasPreorder = false;
    // Mốc thời điểm tạo link thanh toán cọc (để tự hủy nếu quá hạn). Chỉ đặt khi thanh toán PayOS.
    @Column(name = "deposit_requested_at") private LocalDateTime depositRequestedAt;
    private String note;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @jakarta.persistence.PrePersist
    void onInsert() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }
    @jakarta.persistence.PreUpdate
    void onUpdate() { updatedAt = java.time.LocalDateTime.now(); }

    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public UserEntity getUser(){return user;} public void setUser(UserEntity user){this.user=user;}
    public RestaurantTableEntity getTable(){return table;} public void setTable(RestaurantTableEntity table){this.table=table;}
    public String getReservationCode(){return reservationCode;} public void setReservationCode(String reservationCode){this.reservationCode=reservationCode;}
    public String getGuestName(){return guestName;} public void setGuestName(String guestName){this.guestName=guestName;}
    public String getGuestPhone(){return guestPhone;} public void setGuestPhone(String guestPhone){this.guestPhone=guestPhone;}
    public Integer getPartySize(){return partySize;} public void setPartySize(Integer partySize){this.partySize=partySize;}
    public LocalDateTime getReservationTime(){return reservationTime;} public void setReservationTime(LocalDateTime reservationTime){this.reservationTime=reservationTime;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
    public BigDecimal getDepositAmount(){return depositAmount;} public void setDepositAmount(BigDecimal depositAmount){this.depositAmount=depositAmount;}
    public String getDepositStatus(){return depositStatus;} public void setDepositStatus(String depositStatus){this.depositStatus=depositStatus;}
    public String getPaymentMethod(){return paymentMethod;} public void setPaymentMethod(String paymentMethod){this.paymentMethod=paymentMethod;}
    public Boolean getHasPreorder(){return hasPreorder;} public void setHasPreorder(Boolean hasPreorder){this.hasPreorder=hasPreorder;}
    public LocalDateTime getDepositRequestedAt(){return depositRequestedAt;} public void setDepositRequestedAt(LocalDateTime depositRequestedAt){this.depositRequestedAt=depositRequestedAt;}
    public String getNote(){return note;} public void setNote(String note){this.note=note;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime createdAt){this.createdAt=createdAt;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime updatedAt){this.updatedAt=updatedAt;}
}
