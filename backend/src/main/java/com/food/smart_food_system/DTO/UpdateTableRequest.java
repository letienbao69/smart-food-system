package com.food.smart_food_system.DTO;

import jakarta.validation.constraints.Min;

public class UpdateTableRequest {
    private String tableNumber;
    @Min(1) private Integer capacity;
    private String zone;
    private String status;
    private String description;

    public String getTableNumber(){return tableNumber;} public void setTableNumber(String tableNumber){this.tableNumber=tableNumber;}
    public Integer getCapacity(){return capacity;} public void setCapacity(Integer capacity){this.capacity=capacity;}
    public String getZone(){return zone;} public void setZone(String zone){this.zone=zone;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
    public String getDescription(){return description;} public void setDescription(String description){this.description=description;}
}
