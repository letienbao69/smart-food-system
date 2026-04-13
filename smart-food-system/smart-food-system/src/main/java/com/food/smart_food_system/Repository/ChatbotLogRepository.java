package com.food.smart_food_system.Repository;

import com.food.smart_food_system.Entity.ChatbotLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatbotLogRepository extends JpaRepository<ChatbotLogEntity, Long> {
    List<ChatbotLogEntity> findByUserId(Long userId);
}