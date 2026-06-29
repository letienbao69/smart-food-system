package com.food.smart_food_system.Repository;

import com.food.smart_food_system.Entity.EmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<EmployeeEntity, Long> {
    boolean existsByEmployeeCode(String employeeCode);
}
