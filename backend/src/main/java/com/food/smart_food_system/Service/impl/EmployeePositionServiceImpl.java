package com.food.smart_food_system.Service.impl;

import com.food.smart_food_system.DTO.CreateEmployeePositionRequest;
import com.food.smart_food_system.DTO.EmployeePositionDTO;
import com.food.smart_food_system.DTO.UpdateEmployeePositionRequest;
import com.food.smart_food_system.Entity.EmployeePositionEntity;
import com.food.smart_food_system.Exception.ResourceNotFoundException;
import com.food.smart_food_system.Repository.EmployeePositionRepository;
import com.food.smart_food_system.Service.EmployeePositionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class EmployeePositionServiceImpl implements EmployeePositionService {
    private final EmployeePositionRepository repository;
    public EmployeePositionServiceImpl(EmployeePositionRepository repository) { this.repository = repository; }

    @Override public List<EmployeePositionDTO> getAll() { return repository.findAll().stream().map(this::toDto).toList(); }
    @Override public EmployeePositionDTO getById(Long id) { return toDto(find(id)); }

    @Override
    public EmployeePositionDTO create(CreateEmployeePositionRequest request) {
        EmployeePositionEntity e = new EmployeePositionEntity();
        apply(e, request);
        return toDto(repository.save(e));
    }

    @Override
    public EmployeePositionDTO update(Long id, UpdateEmployeePositionRequest request) {
        EmployeePositionEntity e = find(id);
        apply(e, request);
        return toDto(repository.save(e));
    }

    @Override public void delete(Long id) { repository.delete(find(id)); }

    private EmployeePositionEntity find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chức vụ"));
    }

    private void apply(EmployeePositionEntity e, CreateEmployeePositionRequest request) {
        e.setPositionName(request.getPositionName());
        e.setDescription(request.getDescription());
        e.setBaseSalary(request.getBaseSalary() == null ? BigDecimal.ZERO : request.getBaseSalary());
        e.setStatus(request.getStatus() == null || request.getStatus().isBlank() ? "ACTIVE" : request.getStatus());
    }

    private EmployeePositionDTO toDto(EmployeePositionEntity e) {
        EmployeePositionDTO dto = new EmployeePositionDTO();
        dto.setId(e.getId()); dto.setPositionName(e.getPositionName()); dto.setDescription(e.getDescription());
        dto.setBaseSalary(e.getBaseSalary()); dto.setStatus(e.getStatus());
        return dto;
    }
}
