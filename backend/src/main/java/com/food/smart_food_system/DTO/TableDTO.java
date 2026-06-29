package com.food.smart_food_system.DTO;

import java.time.LocalDateTime;

public class TableDTO {
    private Long id;
    private String tableNumber;
    private Integer capacity;
    private String zone;
    private String status;
    private String description;
    private LocalDateTime createdAt;
    private Boolean pendingReservation; // đang có đặt bàn chờ xác nhận (PENDING)

    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getTableNumber(){return tableNumber;} public void setTableNumber(String tableNumber){this.tableNumber=tableNumber;}
    public Integer getCapacity(){return capacity;} public void setCapacity(Integer capacity){this.capacity=capacity;}
    public String getZone(){return zone;} public void setZone(String zone){this.zone=zone;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
    public String getDescription(){return description;} public void setDescription(String description){this.description=description;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime createdAt){this.createdAt=createdAt;}
    public Boolean getPendingReservation(){return pendingReservation;} public void setPendingReservation(Boolean pendingReservation){this.pendingReservation=pendingReservation;}
}
