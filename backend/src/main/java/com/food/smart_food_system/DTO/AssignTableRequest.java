package com.food.smart_food_system.DTO;

import jakarta.validation.constraints.NotNull;

/** Admin gán/đổi bàn cho 1 lượt đặt bàn. */
public class AssignTableRequest {
    @NotNull private Long tableId;
    public Long getTableId(){return tableId;} public void setTableId(Long tableId){this.tableId=tableId;}
}
