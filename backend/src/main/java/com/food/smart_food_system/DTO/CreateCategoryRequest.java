package com.food.smart_food_system.DTO;

import jakarta.validation.constraints.NotBlank;

public class CreateCategoryRequest {
    @NotBlank private String name;
    private String description;
    private String status;
    private Boolean featured;
    public String getName(){return name;} public void setName(String name){this.name=name;}
    public String getDescription(){return description;} public void setDescription(String description){this.description=description;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
    public Boolean getFeatured(){return featured;} public void setFeatured(Boolean featured){this.featured=featured;}
}
