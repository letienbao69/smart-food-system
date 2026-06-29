package com.food.smart_food_system.DTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CreateReviewRequest {
    @NotNull private Long foodId;
    @NotNull private Long orderId;
    @NotNull @Min(1) @Max(5) private Integer rating;
    private String comment;
    private String sentimentLabel;
    public Long getFoodId(){return foodId;} public void setFoodId(Long foodId){this.foodId=foodId;}
    public Long getOrderId(){return orderId;} public void setOrderId(Long orderId){this.orderId=orderId;}
    public Integer getRating(){return rating;} public void setRating(Integer rating){this.rating=rating;}
    public String getComment(){return comment;} public void setComment(String comment){this.comment=comment;}
    public String getSentimentLabel(){return sentimentLabel;} public void setSentimentLabel(String sentimentLabel){this.sentimentLabel=sentimentLabel;}
}
