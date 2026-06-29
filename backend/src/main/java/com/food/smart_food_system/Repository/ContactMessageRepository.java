package com.food.smart_food_system.Repository;

import com.food.smart_food_system.Entity.ContactMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContactMessageRepository extends JpaRepository<ContactMessageEntity, Long> {
    List<ContactMessageEntity> findAllByOrderByCreatedAtDesc();
}
