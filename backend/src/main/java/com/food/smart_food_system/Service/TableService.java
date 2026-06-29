package com.food.smart_food_system.Service;

import com.food.smart_food_system.DTO.CreateTableRequest;
import com.food.smart_food_system.DTO.TableDTO;
import com.food.smart_food_system.DTO.UpdateTableRequest;

import java.util.List;

public interface TableService {
    List<TableDTO> getAll();
    List<TableDTO> getAvailable(Integer minCapacity); // bàn dùng được, đủ sức chứa
    TableDTO getById(Long id);
    TableDTO create(CreateTableRequest request);
    TableDTO update(Long id, UpdateTableRequest request);
    void delete(Long id);
}
