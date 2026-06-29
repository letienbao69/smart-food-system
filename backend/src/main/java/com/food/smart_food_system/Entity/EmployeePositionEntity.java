package com.food.smart_food_system.Entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_positions")
public class EmployeePositionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "position_name", nullable = false, unique = true) private String positionName;
    private String description;
    @Column(name = "base_salary") private BigDecimal baseSalary = BigDecimal.ZERO;
    private String status = "ACTIVE";
    @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getPositionName(){return positionName;} public void setPositionName(String positionName){this.positionName=positionName;}
    public String getDescription(){return description;} public void setDescription(String description){this.description=description;}
    public BigDecimal getBaseSalary(){return baseSalary;} public void setBaseSalary(BigDecimal baseSalary){this.baseSalary=baseSalary;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime createdAt){this.createdAt=createdAt;}
}
