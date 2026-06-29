package com.food.smart_food_system.Service;

import com.food.smart_food_system.DTO.CreateEmployeePositionRequest;
import com.food.smart_food_system.DTO.EmployeePositionDTO;
import com.food.smart_food_system.DTO.UpdateEmployeePositionRequest;
import java.util.List;

public interface EmployeePositionService {
    List<EmployeePositionDTO> getAll();
    EmployeePositionDTO getById(Long id);
    EmployeePositionDTO create(CreateEmployeePositionRequest request);
    EmployeePositionDTO update(Long id, UpdateEmployeePositionRequest request);
    void delete(Long id);
}
