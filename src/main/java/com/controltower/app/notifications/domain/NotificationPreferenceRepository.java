package com.controltower.app.notifications.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    List<NotificationPreference> findByUserId(UUID userId);

    Optional<NotificationPreference> findByUserIdAndNotificationType(UUID userId, String notificationType);

    default boolean isEnabled(UUID userId, String notificationType) {
        return findByUserIdAndNotificationType(userId, notificationType)
                .map(NotificationPreference::isEnabled)
                .orElse(true); // default: enabled
    }
}
