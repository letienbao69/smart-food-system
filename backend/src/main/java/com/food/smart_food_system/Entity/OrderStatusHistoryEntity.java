package com.food.smart_food_system.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_status_history")
public class OrderStatusHistoryEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id", nullable = false) private OrderEntity order;
    private String status;
    @Column(name = "changed_at", insertable = false, updatable = false) private LocalDateTime changedAt;
    private String note;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public OrderEntity getOrder(){return order;} public void setOrder(OrderEntity order){this.order=order;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
    public LocalDateTime getChangedAt(){return changedAt;} public void setChangedAt(LocalDateTime changedAt){this.changedAt=changedAt;}
    public String getNote(){return note;} public void setNote(String note){this.note=note;}
}
