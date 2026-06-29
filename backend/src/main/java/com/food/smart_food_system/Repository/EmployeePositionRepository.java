package com.food.smart_food_system.Repository;

import com.food.smart_food_system.Entity.EmployeePositionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeePositionRepository extends JpaRepository<EmployeePositionEntity, Long> {
    boolean existsByPositionName(String positionName);
}
