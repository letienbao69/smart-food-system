package com.food.smart_food_system.Repository;

import com.food.smart_food_system.Entity.VoucherEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoucherRepository extends JpaRepository<VoucherEntity, Long> {
    Optional<VoucherEntity> findByCode(String code);
    boolean existsByCode(String code);
}