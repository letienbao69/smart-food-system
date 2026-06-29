package com.food.smart_food_system.DTO;

import java.math.BigDecimal;

public class EmployeePositionDTO {
    private Long id; private String positionName; private String description; private BigDecimal baseSalary; private String status;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getPositionName(){return positionName;} public void setPositionName(String positionName){this.positionName=positionName;}
    public String getDescription(){return description;} public void setDescription(String description){this.description=description;}
    public BigDecimal getBaseSalary(){return baseSalary;} public void setBaseSalary(BigDecimal baseSalary){this.baseSalary=baseSalary;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
}
