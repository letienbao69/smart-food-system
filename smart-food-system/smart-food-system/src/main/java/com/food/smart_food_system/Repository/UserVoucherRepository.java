package com.food.smart_food_system.Repository;

import com.food.smart_food_system.Entity.UserVoucherEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserVoucherRepository extends JpaRepository<UserVoucherEntity, Long> {
    List<UserVoucherEntity> findByUserId(Long userId);
    Optional<UserVoucherEntity> findByUserIdAndVoucherId(Long userId, Long voucherId);
}