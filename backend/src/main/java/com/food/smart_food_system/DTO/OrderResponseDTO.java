package com.food.smart_food_system.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderResponseDTO {
    private Long id;
    private String orderCode;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String paymentMethod;
    private String paymentStatus;
    private String orderStatus;
    private String note;
    private LocalDateTime createdAt;
    private List<OrderItemResponseDTO> items;
    private String customerName;
    // Thông tin đặt bàn gắn với đơn món (rút gọn để tránh lồng vòng)
    private Long reservationId;
    private String reservationCode;
    private String tableNumber;
    private LocalDateTime reservationTime;
    private Integer partySize;
    private String guestPhone;
    private java.math.BigDecimal depositAmount;
    private String depositStatus;

    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getOrderCode(){return orderCode;} public void setOrderCode(String orderCode){this.orderCode=orderCode;}
    public BigDecimal getTotalAmount(){return totalAmount;} public void setTotalAmount(BigDecimal totalAmount){this.totalAmount=totalAmount;}
    public BigDecimal getDiscountAmount(){return discountAmount;} public void setDiscountAmount(BigDecimal discountAmount){this.discountAmount=discountAmount;}
    public BigDecimal getFinalAmount(){return finalAmount;} public void setFinalAmount(BigDecimal finalAmount){this.finalAmount=finalAmount;}
    public String getPaymentMethod(){return paymentMethod;} public void setPaymentMethod(String paymentMethod){this.paymentMethod=paymentMethod;}
    public String getPaymentStatus(){return paymentStatus;} public void setPaymentStatus(String paymentStatus){this.paymentStatus=paymentStatus;}
    public String getOrderStatus(){return orderStatus;} public void setOrderStatus(String orderStatus){this.orderStatus=orderStatus;}
    public String getNote(){return note;} public void setNote(String note){this.note=note;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime createdAt){this.createdAt=createdAt;}
    public List<OrderItemResponseDTO> getItems(){return items;} public void setItems(List<OrderItemResponseDTO> items){this.items=items;}
    public String getCustomerName(){return customerName;} public void setCustomerName(String customerName){this.customerName=customerName;}
    public Long getReservationId(){return reservationId;} public void setReservationId(Long reservationId){this.reservationId=reservationId;}
    public String getReservationCode(){return reservationCode;} public void setReservationCode(String reservationCode){this.reservationCode=reservationCode;}
    public String getTableNumber(){return tableNumber;} public void setTableNumber(String tableNumber){this.tableNumber=tableNumber;}
    public LocalDateTime getReservationTime(){return reservationTime;} public void setReservationTime(LocalDateTime reservationTime){this.reservationTime=reservationTime;}
    public Integer getPartySize(){return partySize;} public void setPartySize(Integer partySize){this.partySize=partySize;}
    public String getGuestPhone(){return guestPhone;} public void setGuestPhone(String guestPhone){this.guestPhone=guestPhone;}
    public java.math.BigDecimal getDepositAmount(){return depositAmount;} public void setDepositAmount(java.math.BigDecimal depositAmount){this.depositAmount=depositAmount;}
    public String getDepositStatus(){return depositStatus;} public void setDepositStatus(String depositStatus){this.depositStatus=depositStatus;}
}
