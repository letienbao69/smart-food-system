package com.food.smart_food_system.DTO;

public class CategoryDTO {
    private Long id; private String name; private String description; private String status; private Boolean featured;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getName(){return name;} public void setName(String name){this.name=name;}
    public String getDescription(){return description;} public void setDescription(String description){this.description=description;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
    public Boolean getFeatured(){return featured;} public void setFeatured(Boolean featured){this.featured=featured;}
}
