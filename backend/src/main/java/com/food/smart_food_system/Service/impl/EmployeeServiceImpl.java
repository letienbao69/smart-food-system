package com.food.smart_food_system.Service.impl;

import com.food.smart_food_system.DTO.CreateEmployeeRequest;
import com.food.smart_food_system.DTO.EmployeeDTO;
import com.food.smart_food_system.DTO.UpdateEmployeeRequest;
import com.food.smart_food_system.Entity.EmployeeEntity;
import com.food.smart_food_system.Entity.EmployeePositionEntity;
import com.food.smart_food_system.Exception.ResourceNotFoundException;
import com.food.smart_food_system.Repository.EmployeePositionRepository;
import com.food.smart_food_system.Repository.EmployeeRepository;
import com.food.smart_food_system.Service.EmployeeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class EmployeeServiceImpl implements EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final EmployeePositionRepository positionRepository;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository, EmployeePositionRepository positionRepository) {
        this.employeeRepository = employeeRepository;
        this.positionRepository = positionRepository;
    }

    @Override public List<EmployeeDTO> getAll() { return employeeRepository.findAll().stream().map(this::toDto).toList(); }
    @Override public EmployeeDTO getById(Long id) { return toDto(find(id)); }

    @Override
    public EmployeeDTO create(CreateEmployeeRequest request) {
        EmployeeEntity entity = new EmployeeEntity();
        apply(entity, request);
        return toDto(employeeRepository.save(entity));
    }

    @Override
    public EmployeeDTO update(Long id, UpdateEmployeeRequest request) {
        EmployeeEntity entity = find(id);
        apply(entity, request);
        return toDto(employeeRepository.save(entity));
    }

    @Override public void delete(Long id) { employeeRepository.delete(find(id)); }

    private void apply(EmployeeEntity entity, CreateEmployeeRequest request) {
        EmployeePositionEntity position = positionRepository.findById(request.getPositionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chức vụ"));
        entity.setEmployeeCode(request.getEmployeeCode());
        entity.setFullName(request.getFullName());
        entity.setGender(request.getGender());
        entity.setDateOfBirth(request.getDateOfBirth());
        entity.setPhone(request.getPhone());
        entity.setEmail(request.getEmail());
        entity.setAddress(request.getAddress());
        entity.setHireDate(request.getHireDate());
        entity.setPosition(position);
        entity.setSalary(request.getSalary() == null ? BigDecimal.ZERO : request.getSalary());
        entity.setShiftName(request.getShiftName());
        entity.setStatus(request.getStatus() == null || request.getStatus().isBlank() ? "WORKING" : request.getStatus());
        entity.setNote(request.getNote());
    }

    private EmployeeEntity find(Long id) {
        return employeeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên"));
    }

    private EmployeeDTO toDto(EmployeeEntity e) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setId(e.getId()); dto.setEmployeeCode(e.getEmployeeCode()); dto.setFullName(e.getFullName()); dto.setGender(e.getGender());
        dto.setDateOfBirth(e.getDateOfBirth()); dto.setPhone(e.getPhone()); dto.setEmail(e.getEmail()); dto.setAddress(e.getAddress());
        dto.setHireDate(e.getHireDate()); dto.setPositionId(e.getPosition().getId()); dto.setPositionName(e.getPosition().getPositionName());
        dto.setSalary(e.getSalary()); dto.setShiftName(e.getShiftName()); dto.setStatus(e.getStatus()); dto.setNote(e.getNote());
        return dto;
    }
}
