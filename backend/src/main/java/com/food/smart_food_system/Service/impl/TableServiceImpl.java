package com.food.smart_food_system.Service.impl;

import com.food.smart_food_system.DTO.CreateTableRequest;
import com.food.smart_food_system.DTO.TableDTO;
import com.food.smart_food_system.DTO.UpdateTableRequest;
import com.food.smart_food_system.Entity.RestaurantTableEntity;
import com.food.smart_food_system.Exception.BusinessException;
import com.food.smart_food_system.Exception.ResourceNotFoundException;
import com.food.smart_food_system.Repository.ReservationRepository;
import com.food.smart_food_system.Repository.RestaurantTableRepository;
import com.food.smart_food_system.Service.TableService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TableServiceImpl implements TableService {

    private final RestaurantTableRepository tableRepository;
    private final ReservationRepository reservationRepository;

    public TableServiceImpl(RestaurantTableRepository tableRepository,
                            ReservationRepository reservationRepository) {
        this.tableRepository = tableRepository;
        this.reservationRepository = reservationRepository;
    }

    @Override
    public List<TableDTO> getAll() {
        return tableRepository.findAllByOrderByTableNumberAsc().stream().map(this::toDto).toList();
    }

    @Override
    public List<TableDTO> getAvailable(Integer minCapacity) {
        int min = (minCapacity == null || minCapacity < 1) ? 1 : minCapacity;
        return tableRepository
                .findByCapacityGreaterThanEqualAndStatusOrderByCapacityAsc(min, "AVAILABLE")
                .stream().map(this::toDto).toList();
    }

    @Override
    public TableDTO getById(Long id) {
        return toDto(require(id));
    }

    @Override
    public TableDTO create(CreateTableRequest request) {
        if (tableRepository.existsByTableNumber(request.getTableNumber().trim())) {
            throw new BusinessException("Số bàn đã tồn tại: " + request.getTableNumber());
        }
        RestaurantTableEntity t = new RestaurantTableEntity();
        t.setTableNumber(request.getTableNumber().trim());
        t.setCapacity(request.getCapacity());
        t.setZone(request.getZone());
        t.setStatus(normalizeStatus(request.getStatus()));
        t.setDescription(request.getDescription());
        return toDto(tableRepository.save(t));
    }

    @Override
    public TableDTO update(Long id, UpdateTableRequest request) {
        RestaurantTableEntity t = require(id);
        if (request.getTableNumber() != null && !request.getTableNumber().isBlank()) {
            String newNumber = request.getTableNumber().trim();
            if (!newNumber.equalsIgnoreCase(t.getTableNumber()) && tableRepository.existsByTableNumber(newNumber)) {
                throw new BusinessException("Số bàn đã tồn tại: " + newNumber);
            }
            t.setTableNumber(newNumber);
        }
        if (request.getCapacity() != null) t.setCapacity(request.getCapacity());
        if (request.getZone() != null) t.setZone(request.getZone());
        if (request.getStatus() != null && !request.getStatus().isBlank()) t.setStatus(normalizeStatus(request.getStatus()));
        if (request.getDescription() != null) t.setDescription(request.getDescription());
        return toDto(tableRepository.save(t));
    }

    @Override
    public void delete(Long id) {
        RestaurantTableEntity t = require(id);
        // Không cho xóa bàn đang gắn với lượt đặt bàn chưa hoàn tất
        boolean inUse = reservationRepository.findAll().stream()
                .anyMatch(r -> r.getTable() != null
                        && r.getTable().getId().equals(id)
                        && List.of("PENDING", "CONFIRMED", "SEATED").contains(r.getStatus()));
        if (inUse) {
            throw new BusinessException("Bàn đang có lượt đặt chưa hoàn tất, không thể xóa. Hãy chuyển sang trạng thái Bảo trì.");
        }
        tableRepository.delete(t);
    }

    private RestaurantTableEntity require(Long id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bàn"));
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return "AVAILABLE";
        String s = status.trim().toUpperCase();
        if (!List.of("AVAILABLE", "MAINTENANCE", "OCCUPIED", "RESERVED").contains(s)) {
            throw new BusinessException("Trạng thái bàn không hợp lệ: " + status);
        }
        return s;
    }

    private TableDTO toDto(RestaurantTableEntity t) {
        TableDTO dto = new TableDTO();
        dto.setId(t.getId());
        dto.setTableNumber(t.getTableNumber());
        dto.setCapacity(t.getCapacity());
        dto.setZone(t.getZone());
        dto.setStatus(t.getStatus());
        dto.setDescription(t.getDescription());
        dto.setCreatedAt(t.getCreatedAt());
        dto.setPendingReservation(reservationRepository.existsByTableIdAndStatus(t.getId(), "PENDING"));
        return dto;
    }
}
