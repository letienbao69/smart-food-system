package com.food.smart_food_system.DTO;

import java.math.BigDecimal;

public class ReportSummaryDTO {
    private Long totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal paidRevenue;
    private BigDecimal unpaidRevenue;
    private Long pendingOrders;
    private Long completedOrders;
    private Long cancelledOrders;
    private Long totalUsers;
    private Long totalFoods;

    public ReportSummaryDTO() {}

    public Long getTotalOrders(){return totalOrders;} public void setTotalOrders(Long v){totalOrders=v;}
    public BigDecimal getTotalRevenue(){return totalRevenue;} public void setTotalRevenue(BigDecimal v){totalRevenue=v;}
    public BigDecimal getPaidRevenue(){return paidRevenue;} public void setPaidRevenue(BigDecimal v){paidRevenue=v;}
    public BigDecimal getUnpaidRevenue(){return unpaidRevenue;} public void setUnpaidRevenue(BigDecimal v){unpaidRevenue=v;}
    public Long getPendingOrders(){return pendingOrders;} public void setPendingOrders(Long v){pendingOrders=v;}
    public Long getCompletedOrders(){return completedOrders;} public void setCompletedOrders(Long v){completedOrders=v;}
    public Long getCancelledOrders(){return cancelledOrders;} public void setCancelledOrders(Long v){cancelledOrders=v;}
    public Long getTotalUsers(){return totalUsers;} public void setTotalUsers(Long v){totalUsers=v;}
    public Long getTotalFoods(){return totalFoods;} public void setTotalFoods(Long v){totalFoods=v;}
}
