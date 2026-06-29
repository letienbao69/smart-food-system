package com.food.smart_food_system.Repository;

import com.food.smart_food_system.Entity.UserVoucherEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserVoucherRepository extends JpaRepository<UserVoucherEntity, Long> {
}
