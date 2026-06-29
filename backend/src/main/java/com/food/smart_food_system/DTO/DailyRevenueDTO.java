package com.food.smart_food_system.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DailyRevenueDTO {
    private LocalDate date;
    private BigDecimal revenue;
    private Long totalOrders;

    public DailyRevenueDTO() {
    }

    public DailyRevenueDTO(LocalDate date, BigDecimal revenue, Long totalOrders) {
        this.date = date;
        this.revenue = revenue;
        this.totalOrders = totalOrders;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public Long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Long totalOrders) {
        this.totalOrders = totalOrders;
    }
}