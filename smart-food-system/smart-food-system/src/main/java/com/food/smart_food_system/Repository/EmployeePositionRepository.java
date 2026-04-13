package com.food.smart_food_system.Repository;

import com.food.smart_food_system.Entity.EmployeePositionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeePositionRepository extends JpaRepository<EmployeePositionEntity, Long> {
    Optional<EmployeePositionEntity> findByPositionName(String positionName);
    boolean existsByPositionName(String positionName);
}