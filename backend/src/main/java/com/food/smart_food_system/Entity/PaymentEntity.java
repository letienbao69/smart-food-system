package com.food.smart_food_system.Entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class PaymentEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id", nullable = false) private OrderEntity order;
    @Column(name = "transaction_code") private String transactionCode;
    private String provider;
    private BigDecimal amount;
    private String status = "PENDING";
    @Column(name = "paid_at") private LocalDateTime paidAt;
    @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public OrderEntity getOrder(){return order;} public void setOrder(OrderEntity order){this.order=order;}
    public String getTransactionCode(){return transactionCode;} public void setTransactionCode(String transactionCode){this.transactionCode=transactionCode;}
    public String getProvider(){return provider;} public void setProvider(String provider){this.provider=provider;}
    public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal amount){this.amount=amount;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
    public LocalDateTime getPaidAt(){return paidAt;} public void setPaidAt(LocalDateTime paidAt){this.paidAt=paidAt;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime createdAt){this.createdAt=createdAt;}
}
