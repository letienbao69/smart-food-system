package com.food.smart_food_system.Entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items")
public class CartItemEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cart_id", nullable = false)
    private CartEntity cart;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "food_id", nullable = false)
    private FoodEntity food;
    private Integer quantity;
    @Column(name = "unit_price") private BigDecimal unitPrice;
    @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public CartEntity getCart(){return cart;} public void setCart(CartEntity cart){this.cart=cart;}
    public FoodEntity getFood(){return food;} public void setFood(FoodEntity food){this.food=food;}
    public Integer getQuantity(){return quantity;} public void setQuantity(Integer quantity){this.quantity=quantity;}
    public BigDecimal getUnitPrice(){return unitPrice;} public void setUnitPrice(BigDecimal unitPrice){this.unitPrice=unitPrice;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime createdAt){this.createdAt=createdAt;}
}
