package com.food.smart_food_system.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "order_status_history")
public class OrderStatusHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt;

    @Column(name = "note", length = 255)
    private String note;

    @PrePersist
    public void prePersist() {
        this.changedAt = LocalDateTime.now();
    }
}