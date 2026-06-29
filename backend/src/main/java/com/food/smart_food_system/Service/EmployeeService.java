package com.food.smart_food_system.Service;

import com.food.smart_food_system.DTO.CreateEmployeeRequest;
import com.food.smart_food_system.DTO.EmployeeDTO;
import com.food.smart_food_system.DTO.UpdateEmployeeRequest;
import java.util.List;

public interface EmployeeService {
    List<EmployeeDTO> getAll();
    EmployeeDTO getById(Long id);
    EmployeeDTO create(CreateEmployeeRequest request);
    EmployeeDTO update(Long id, UpdateEmployeeRequest request);
    void delete(Long id);
}
