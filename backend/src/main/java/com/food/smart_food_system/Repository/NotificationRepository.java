package com.food.smart_food_system.Repository;

import com.food.smart_food_system.Entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity> findByTargetRoleOrderByCreatedAtDesc(String targetRole);

    List<NotificationEntity> findByTargetRoleAndReadStatusOrderByCreatedAtDesc(
            String targetRole,
            Boolean readStatus
    );

    Long countByTargetRoleAndReadStatus(String targetRole, Boolean readStatus);

    Optional<NotificationEntity> findByIdAndTargetRole(Long id, String targetRole);

    List<NotificationEntity> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId);

    List<NotificationEntity> findByTargetUserIdAndReadStatusOrderByCreatedAtDesc(
            Long targetUserId,
            Boolean readStatus
    );

    List<NotificationEntity> findByTargetUserIdAndReadStatus(
            Long targetUserId,
            Boolean readStatus
    );

    Long countByTargetUserIdAndReadStatus(Long targetUserId, Boolean readStatus);

    Optional<NotificationEntity> findByIdAndTargetUserId(Long id, Long targetUserId);
}