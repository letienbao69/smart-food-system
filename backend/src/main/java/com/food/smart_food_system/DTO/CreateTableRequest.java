package com.food.smart_food_system.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateTableRequest {
    @NotBlank private String tableNumber;
    @NotNull @Min(1) private Integer capacity;
    private String zone;
    private String status;       // mặc định AVAILABLE
    private String description;

    public String getTableNumber(){return tableNumber;} public void setTableNumber(String tableNumber){this.tableNumber=tableNumber;}
    public Integer getCapacity(){return capacity;} public void setCapacity(Integer capacity){this.capacity=capacity;}
    public String getZone(){return zone;} public void setZone(String zone){this.zone=zone;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
    public String getDescription(){return description;} public void setDescription(String description){this.description=description;}
}
