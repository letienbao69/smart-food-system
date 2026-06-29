package com.food.smart_food_system.DTO;

import java.math.BigDecimal;

public class PaymentStatisticDTO {

    private String status;
    private Long totalPayments;
    private BigDecimal totalAmount;

    public PaymentStatisticDTO() {
    }

    public PaymentStatisticDTO(String status, Long totalPayments, BigDecimal totalAmount) {
        this.status = status;
        this.totalPayments = totalPayments;
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public Long getTotalPayments() {
        return totalPayments;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTotalPayments(Long totalPayments) {
        this.totalPayments = totalPayments;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}