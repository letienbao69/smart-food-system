package com.food.smart_food_system.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
public class ReviewEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private UserEntity user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "food_id", nullable = false) private FoodEntity food;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id", nullable = false) private OrderEntity order;
    private Integer rating;
    @Column(columnDefinition = "TEXT") private String comment;
    @Column(name = "sentiment_label") private String sentimentLabel;
    @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public UserEntity getUser(){return user;} public void setUser(UserEntity user){this.user=user;}
    public FoodEntity getFood(){return food;} public void setFood(FoodEntity food){this.food=food;}
    public OrderEntity getOrder(){return order;} public void setOrder(OrderEntity order){this.order=order;}
    public Integer getRating(){return rating;} public void setRating(Integer rating){this.rating=rating;}
    public String getComment(){return comment;} public void setComment(String comment){this.comment=comment;}
    public String getSentimentLabel(){return sentimentLabel;} public void setSentimentLabel(String sentimentLabel){this.sentimentLabel=sentimentLabel;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime createdAt){this.createdAt=createdAt;}
}
