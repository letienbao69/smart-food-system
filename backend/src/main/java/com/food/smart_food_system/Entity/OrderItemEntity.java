package com.food.smart_food_system.Entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItemEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id", nullable = false) private OrderEntity order;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "food_id", nullable = false) private FoodEntity food;
    @Column(name = "food_name", nullable = false) private String foodName;
    private Integer quantity;
    @Column(name = "unit_price") private BigDecimal unitPrice;
    private BigDecimal subtotal;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public OrderEntity getOrder(){return order;} public void setOrder(OrderEntity order){this.order=order;}
    public FoodEntity getFood(){return food;} public void setFood(FoodEntity food){this.food=food;}
    public String getFoodName(){return foodName;} public void setFoodName(String foodName){this.foodName=foodName;}
    public Integer getQuantity(){return quantity;} public void setQuantity(Integer quantity){this.quantity=quantity;}
    public BigDecimal getUnitPrice(){return unitPrice;} public void setUnitPrice(BigDecimal unitPrice){this.unitPrice=unitPrice;}
    public BigDecimal getSubtotal(){return subtotal;} public void setSubtotal(BigDecimal subtotal){this.subtotal=subtotal;}
}
