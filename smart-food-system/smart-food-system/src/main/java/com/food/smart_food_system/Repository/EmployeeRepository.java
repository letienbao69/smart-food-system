package com.food.smart_food_system.Repository;

import com.food.smart_food_system.Entity.EmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<EmployeeEntity, Long> {
    Optional<EmployeeEntity> findByEmployeeCode(String employeeCode);
    Optional<EmployeeEntity> findByEmail(String email);
    Optional<EmployeeEntity> findByPhone(String phone);
    List<EmployeeEntity> findByStatus(String status);
}