package com.food.smart_food_system.Repository;

import com.food.smart_food_system.Entity.AddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<AddressEntity, Long> {
    List<AddressEntity> findByUserId(Long userId);
    Optional<AddressEntity> findByUserIdAndIsDefaultTrue(Long userId);
}