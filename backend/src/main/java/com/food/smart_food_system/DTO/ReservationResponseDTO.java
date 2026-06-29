package com.food.smart_food_system.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReservationResponseDTO {
    private Long id;
    private String reservationCode;
    private String guestName;
    private String guestPhone;
    private Integer partySize;
    private LocalDateTime reservationTime;
    private String status;
    private BigDecimal depositAmount;
    private String depositStatus;
    private String paymentMethod;
    private Boolean hasPreorder;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime depositExpiresAt;
    private String customerName;
    private TableDTO table;
    // Đơn món đặt trước gắn với lượt đặt bàn này (nếu có)
    private OrderResponseDTO preorder;

    public Long getId(){return id;} public void setId(Long id){this.id=id;}
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
    public String getNote(){return note;} public void setNote(String note){this.note=note;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime createdAt){this.createdAt=createdAt;}
    public LocalDateTime getDepositExpiresAt(){return depositExpiresAt;} public void setDepositExpiresAt(LocalDateTime depositExpiresAt){this.depositExpiresAt=depositExpiresAt;}
    public String getCustomerName(){return customerName;} public void setCustomerName(String customerName){this.customerName=customerName;}
    public TableDTO getTable(){return table;} public void setTable(TableDTO table){this.table=table;}
    public OrderResponseDTO getPreorder(){return preorder;} public void setPreorder(OrderResponseDTO preorder){this.preorder=preorder;}
}
