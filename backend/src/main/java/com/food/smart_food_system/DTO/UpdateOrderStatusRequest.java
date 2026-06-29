package com.food.smart_food_system.DTO;

public class UpdateOrderStatusRequest {
    // Không bắt buộc - admin có thể chỉ cập nhật paymentStatus mà không đổi orderStatus
    private String orderStatus;
    private String paymentStatus;
    private String note;
    public String getOrderStatus(){return orderStatus;} public void setOrderStatus(String orderStatus){this.orderStatus=orderStatus;}
    public String getPaymentStatus(){return paymentStatus;} public void setPaymentStatus(String paymentStatus){this.paymentStatus=paymentStatus;}
    public String getNote(){return note;} public void setNote(String note){this.note=note;}
}
