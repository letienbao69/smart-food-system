package com.food.smart_food_system.Service;

import com.food.smart_food_system.DTO.CreateVoucherRequest;
import com.food.smart_food_system.DTO.UpdateVoucherRequest;
import com.food.smart_food_system.DTO.VoucherDTO;
import java.util.List;

public interface VoucherService {
    List<VoucherDTO> getAll();
    List<VoucherDTO> getActive();
    VoucherDTO getById(Long id);
    VoucherDTO create(CreateVoucherRequest request);
    VoucherDTO update(Long id, UpdateVoucherRequest request);
    void delete(Long id);
    VoucherDTO validateCode(String code);
}
