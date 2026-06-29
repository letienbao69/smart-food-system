package com.food.smart_food_system.DTO;

import java.time.LocalDateTime;

public class ReviewDTO {
    private Long id; private Long userId; private String userName; private Long foodId; private String foodName; private Long orderId; private Integer rating; private String comment; private String sentimentLabel; private LocalDateTime createdAt;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getUserId(){return userId;} public void setUserId(Long userId){this.userId=userId;}
    public String getUserName(){return userName;} public void setUserName(String userName){this.userName=userName;}
    public Long getFoodId(){return foodId;} public void setFoodId(Long foodId){this.foodId=foodId;}
    public Long getOrderId(){return orderId;} public void setOrderId(Long orderId){this.orderId=orderId;}
    public Integer getRating(){return rating;} public void setRating(Integer rating){this.rating=rating;}
    public String getComment(){return comment;} public void setComment(String comment){this.comment=comment;}
    public String getSentimentLabel(){return sentimentLabel;} public void setSentimentLabel(String sentimentLabel){this.sentimentLabel=sentimentLabel;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime createdAt){this.createdAt=createdAt;}
    public String getFoodName(){return foodName;} public void setFoodName(String foodName){this.foodName=foodName;}
}
