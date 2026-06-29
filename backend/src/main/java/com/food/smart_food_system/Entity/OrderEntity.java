package com.food.smart_food_system.Entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Đơn món của khách. Trong mô hình ăn tại nhà hàng, mỗi đơn món gắn với
 * một lượt ĐẶT BÀN (reservation) thay vì địa chỉ giao hàng.
 * Đơn món có thể là "đặt trước" (lúc đặt bàn) hoặc do nhân viên thêm khi khách ngồi tại bàn.
 */
@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private UserEntity user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reservation_id") private ReservationEntity reservation;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "voucher_id") private VoucherEntity voucher;
    @Column(name = "order_code", nullable = false, unique = true) private String orderCode;
    @Column(name = "total_amount", nullable = false) private BigDecimal totalAmount;
    @Column(name = "discount_amount") private BigDecimal discountAmount = BigDecimal.ZERO;
    @Column(name = "final_amount", nullable = false) private BigDecimal finalAmount;
    // Hình thức thanh toán phần ăn TẠI QUÁN: CASH | BANK_TRANSFER
    @Column(name = "payment_method", nullable = false) private String paymentMethod;
    @Column(name = "payment_status") private String paymentStatus = "UNPAID";
    // PENDING -> CONFIRMED -> PREPARING -> SERVED -> COMPLETED | CANCELLED
    @Column(name = "order_status") private String orderStatus = "PENDING";
    private String note;
    @Column(name = "created_at") private LocalDateTime createdAt;

    @jakarta.persistence.PrePersist
    void onInsert() {
        if (createdAt == null) createdAt = java.time.LocalDateTime.now();
    }
    @Column(name = "updated_at", insertable = false, updatable = false) private LocalDateTime updatedAt;

    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public UserEntity getUser(){return user;} public void setUser(UserEntity user){this.user=user;}
    public ReservationEntity getReservation(){return reservation;} public void setReservation(ReservationEntity reservation){this.reservation=reservation;}
    public VoucherEntity getVoucher(){return voucher;} public void setVoucher(VoucherEntity voucher){this.voucher=voucher;}
    public String getOrderCode(){return orderCode;} public void setOrderCode(String orderCode){this.orderCode=orderCode;}
    public BigDecimal getTotalAmount(){return totalAmount;} public void setTotalAmount(BigDecimal totalAmount){this.totalAmount=totalAmount;}
    public BigDecimal getDiscountAmount(){return discountAmount;} public void setDiscountAmount(BigDecimal discountAmount){this.discountAmount=discountAmount;}
    public BigDecimal getFinalAmount(){return finalAmount;} public void setFinalAmount(BigDecimal finalAmount){this.finalAmount=finalAmount;}
    public String getPaymentMethod(){return paymentMethod;} public void setPaymentMethod(String paymentMethod){this.paymentMethod=paymentMethod;}
    public String getPaymentStatus(){return paymentStatus;} public void setPaymentStatus(String paymentStatus){this.paymentStatus=paymentStatus;}
    public String getOrderStatus(){return orderStatus;} public void setOrderStatus(String orderStatus){this.orderStatus=orderStatus;}
    public String getNote(){return note;} public void setNote(String note){this.note=note;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime createdAt){this.createdAt=createdAt;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime updatedAt){this.updatedAt=updatedAt;}
}
