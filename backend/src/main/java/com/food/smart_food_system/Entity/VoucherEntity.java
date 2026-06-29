package com.food.smart_food_system.Entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
public class VoucherEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true) private String code;
    @Column(nullable = false) private String name;
    @Column(name = "discount_type", nullable = false) private String discountType;
    @Column(name = "discount_value", nullable = false) private BigDecimal discountValue;
    @Column(name = "min_order_value") private BigDecimal minOrderValue = BigDecimal.ZERO;
    @Column(name = "max_discount") private BigDecimal maxDiscount;
    private Integer quantity = 0;
    @Column(name = "start_date") private LocalDateTime startDate;
    @Column(name = "end_date") private LocalDateTime endDate;
    private String status = "ACTIVE";
    @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getCode(){return code;} public void setCode(String code){this.code=code;}
    public String getName(){return name;} public void setName(String name){this.name=name;}
    public String getDiscountType(){return discountType;} public void setDiscountType(String discountType){this.discountType=discountType;}
    /** Voucher giảm theo phần trăm. Chấp nhận cả hai quy ước lưu trữ: '%' và 'PERCENT'. */
    public boolean isPercent(){ return "%".equals(discountType) || "PERCENT".equalsIgnoreCase(discountType); }
    public BigDecimal getDiscountValue(){return discountValue;} public void setDiscountValue(BigDecimal discountValue){this.discountValue=discountValue;}
    public BigDecimal getMinOrderValue(){return minOrderValue;} public void setMinOrderValue(BigDecimal minOrderValue){this.minOrderValue=minOrderValue;}
    public BigDecimal getMaxDiscount(){return maxDiscount;} public void setMaxDiscount(BigDecimal maxDiscount){this.maxDiscount=maxDiscount;}
    public Integer getQuantity(){return quantity;} public void setQuantity(Integer quantity){this.quantity=quantity;}
    public LocalDateTime getStartDate(){return startDate;} public void setStartDate(LocalDateTime startDate){this.startDate=startDate;}
    public LocalDateTime getEndDate(){return endDate;} public void setEndDate(LocalDateTime endDate){this.endDate=endDate;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime createdAt){this.createdAt=createdAt;}
}
