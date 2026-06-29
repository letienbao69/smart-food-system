package com.food.smart_food_system.DTO;

/** Admin cập nhật trạng thái đặt bàn / đặt cọc. */
public class UpdateReservationStatusRequest {
    private String status;          // CONFIRMED | SEATED | COMPLETED | CANCELLED | NO_SHOW
    private String depositStatus;   // NONE | PENDING | PAID
    private Long tableId;           // gán bàn kèm theo (tùy chọn)
    private String note;

    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
    public String getDepositStatus(){return depositStatus;} public void setDepositStatus(String depositStatus){this.depositStatus=depositStatus;}
    public Long getTableId(){return tableId;} public void setTableId(Long tableId){this.tableId=tableId;}
    public String getNote(){return note;} public void setNote(String note){this.note=note;}
}
