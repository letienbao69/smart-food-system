package com.food.smart_food_system.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VoucherDTO {
    private Long id; private String code; private String name; private String discountType; private BigDecimal discountValue;
    private BigDecimal minOrderValue; private BigDecimal maxDiscount; private Integer quantity; private LocalDateTime startDate; private LocalDateTime endDate; private String status;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getCode(){return code;} public void setCode(String code){this.code=code;}
    public String getName(){return name;} public void setName(String name){this.name=name;}
    public String getDiscountType(){return discountType;} public void setDiscountType(String discountType){this.discountType=discountType;}
    public BigDecimal getDiscountValue(){return discountValue;} public void setDiscountValue(BigDecimal discountValue){this.discountValue=discountValue;}
    public BigDecimal getMinOrderValue(){return minOrderValue;} public void setMinOrderValue(BigDecimal minOrderValue){this.minOrderValue=minOrderValue;}
    public BigDecimal getMaxDiscount(){return maxDiscount;} public void setMaxDiscount(BigDecimal maxDiscount){this.maxDiscount=maxDiscount;}
    public Integer getQuantity(){return quantity;} public void setQuantity(Integer quantity){this.quantity=quantity;}
    public LocalDateTime getStartDate(){return startDate;} public void setStartDate(LocalDateTime startDate){this.startDate=startDate;}
    public LocalDateTime getEndDate(){return endDate;} public void setEndDate(LocalDateTime endDate){this.endDate=endDate;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
}
