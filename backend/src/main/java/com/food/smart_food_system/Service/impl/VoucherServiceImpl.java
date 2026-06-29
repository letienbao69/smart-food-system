package com.food.smart_food_system.Service.impl;

import com.food.smart_food_system.DTO.CreateVoucherRequest;
import com.food.smart_food_system.DTO.UpdateVoucherRequest;
import com.food.smart_food_system.DTO.VoucherDTO;
import com.food.smart_food_system.Entity.VoucherEntity;
import com.food.smart_food_system.Exception.BusinessException;
import com.food.smart_food_system.Exception.ResourceNotFoundException;
import com.food.smart_food_system.Repository.VoucherRepository;
import com.food.smart_food_system.Service.VoucherService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class VoucherServiceImpl implements VoucherService {
    private final VoucherRepository voucherRepository;
    public VoucherServiceImpl(VoucherRepository voucherRepository) { this.voucherRepository = voucherRepository; }

    @Override
    public List<VoucherDTO> getAll() { return voucherRepository.findAll().stream().map(this::toDto).toList(); }

    @Override
    public List<VoucherDTO> getActive() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return voucherRepository.findAll().stream()
                .filter(v -> "ACTIVE".equalsIgnoreCase(v.getStatus()))
                .filter(v -> v.getStartDate() == null || !now.isBefore(v.getStartDate()))
                .filter(v -> v.getEndDate() == null || !now.isAfter(v.getEndDate()))
                .filter(v -> v.getQuantity() == null || v.getQuantity() > 0)
                .map(this::toDto)
                .toList();
    }

    @Override
    public VoucherDTO getById(Long id) { return toDto(find(id)); }

    @Override
    public VoucherDTO create(CreateVoucherRequest request) {
        VoucherEntity entity = new VoucherEntity();
        apply(entity, request);
        return toDto(voucherRepository.save(entity));
    }

    @Override
    public VoucherDTO update(Long id, UpdateVoucherRequest request) {
        VoucherEntity entity = find(id);
        apply(entity, request);
        return toDto(voucherRepository.save(entity));
    }

    @Override
    public void delete(Long id) {
        VoucherEntity voucher = find(id);
        try {
            voucherRepository.delete(voucher);
            voucherRepository.flush(); // force FK check immediately within transaction
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(
                "Không thể xóa voucher \"" + voucher.getCode() + "\" vì đang được sử dụng trong đơn hàng. " +
                "Hãy chuyển trạng thái sang DISABLED thay vì xóa."
            );
        }
    }

    @Override
    public VoucherDTO validateCode(String code) { return toDto(requireValid(code)); }

    public VoucherEntity requireValid(String code) {
        VoucherEntity voucher = voucherRepository.findByCode(code == null ? null : code.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Voucher không tồn tại"));
        LocalDateTime now = LocalDateTime.now();
        if (!"ACTIVE".equalsIgnoreCase(voucher.getStatus())) throw new ResourceNotFoundException("Voucher không khả dụng");
        if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) throw new ResourceNotFoundException("Voucher chưa bắt đầu");
        if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate())) throw new ResourceNotFoundException("Voucher đã hết hạn");
        if (voucher.getQuantity() != null && voucher.getQuantity() <= 0) throw new ResourceNotFoundException("Voucher đã hết lượt");
        return voucher;
    }

    private void apply(VoucherEntity entity, CreateVoucherRequest request) {
        entity.setCode(request.getCode() == null ? null : request.getCode().trim().toUpperCase());
        entity.setName(request.getName());
        entity.setDiscountType(request.getDiscountType());
        entity.setDiscountValue(request.getDiscountValue());
        entity.setMinOrderValue(request.getMinOrderValue() == null ? BigDecimal.ZERO : request.getMinOrderValue());
        entity.setMaxDiscount(request.getMaxDiscount());
        entity.setQuantity(request.getQuantity() == null ? 0 : request.getQuantity());
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setStatus(request.getStatus() == null || request.getStatus().isBlank() ? "ACTIVE" : request.getStatus());
    }

    private VoucherEntity find(Long id) { return voucherRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher")); }

    private VoucherDTO toDto(VoucherEntity e) {
        VoucherDTO dto = new VoucherDTO();
        dto.setId(e.getId()); dto.setCode(e.getCode()); dto.setName(e.getName()); dto.setDiscountType(e.getDiscountType());
        dto.setDiscountValue(e.getDiscountValue()); dto.setMinOrderValue(e.getMinOrderValue()); dto.setMaxDiscount(e.getMaxDiscount());
        dto.setQuantity(e.getQuantity()); dto.setStartDate(e.getStartDate()); dto.setEndDate(e.getEndDate()); dto.setStatus(e.getStatus());
        return dto;
    }
}
