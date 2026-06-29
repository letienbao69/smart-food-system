package com.food.smart_food_system.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Bàn ăn vật lý của nhà hàng.
 * Thay thế hoàn toàn cho khái niệm "địa chỉ giao hàng" của bản delivery cũ.
 */
@Entity
@Table(name = "restaurant_tables")
public class RestaurantTableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "table_number", nullable = false, unique = true) private String tableNumber;
    @Column(nullable = false) private Integer capacity = 2;
    private String zone;                 // Khu vực: "Tầng 1", "VIP", "Sân vườn"...
    @Column(nullable = false) private String status = "AVAILABLE"; // AVAILABLE | MAINTENANCE
    private String description;
    @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;

    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getTableNumber(){return tableNumber;} public void setTableNumber(String tableNumber){this.tableNumber=tableNumber;}
    public Integer getCapacity(){return capacity;} public void setCapacity(Integer capacity){this.capacity=capacity;}
    public String getZone(){return zone;} public void setZone(String zone){this.zone=zone;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
    public String getDescription(){return description;} public void setDescription(String description){this.description=description;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime createdAt){this.createdAt=createdAt;}
}
