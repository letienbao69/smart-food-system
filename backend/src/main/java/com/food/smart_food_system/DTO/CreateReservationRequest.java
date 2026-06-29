package com.food.smart_food_system.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Khách đặt bàn. Linh hoạt:
 *  - preorder = false  -> chỉ giữ bàn, gọi món tại quán.
 *  - preorder = true   -> đặt món trước, hệ thống lấy giỏ hàng tạo đơn món gắn vào đặt bàn.
 */
public class CreateReservationRequest {
    @NotBlank private String guestName;
    @NotBlank private String guestPhone;
    @NotNull @Min(1) private Integer partySize;
    @NotNull private LocalDateTime reservationTime;
    // Khách có thể chọn sẵn 1 bàn mong muốn (tùy chọn); admin có thể đổi khi xác nhận.
    private Long tableId;
    private String note;
    // Thanh toán phần ăn tại quán: CASH | BANK_TRANSFER
    private String paymentMethod;
    // Có đặt món trước hay không
    private Boolean preorder = false;
    // Mã voucher áp cho đơn món đặt trước (nếu có)
    private String voucherCode;

    public String getGuestName(){return guestName;} public void setGuestName(String guestName){this.guestName=guestName;}
    public String getGuestPhone(){return guestPhone;} public void setGuestPhone(String guestPhone){this.guestPhone=guestPhone;}
    public Integer getPartySize(){return partySize;} public void setPartySize(Integer partySize){this.partySize=partySize;}
    public LocalDateTime getReservationTime(){return reservationTime;} public void setReservationTime(LocalDateTime reservationTime){this.reservationTime=reservationTime;}
    public Long getTableId(){return tableId;} public void setTableId(Long tableId){this.tableId=tableId;}
    public String getNote(){return note;} public void setNote(String note){this.note=note;}
    public String getPaymentMethod(){return paymentMethod;} public void setPaymentMethod(String paymentMethod){this.paymentMethod=paymentMethod;}
    public Boolean getPreorder(){return preorder;} public void setPreorder(Boolean preorder){this.preorder=preorder;}
    public String getVoucherCode(){return voucherCode;} public void setVoucherCode(String voucherCode){this.voucherCode=voucherCode;}
}
