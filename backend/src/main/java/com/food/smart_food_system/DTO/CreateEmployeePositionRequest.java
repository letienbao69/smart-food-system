package com.food.smart_food_system.DTO;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public class CreateEmployeePositionRequest {
    @NotBlank private String positionName;
    private String description;
    private BigDecimal baseSalary;
    private String status;
    public String getPositionName(){return positionName;} public void setPositionName(String positionName){this.positionName=positionName;}
    public String getDescription(){return description;} public void setDescription(String description){this.description=description;}
    public BigDecimal getBaseSalary(){return baseSalary;} public void setBaseSalary(BigDecimal baseSalary){this.baseSalary=baseSalary;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
}
