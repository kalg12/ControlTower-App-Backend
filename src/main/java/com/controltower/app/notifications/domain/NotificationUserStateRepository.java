package com.controltower.app.notifications.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationUserStateRepository extends JpaRepository<NotificationUserState, UUID> {

    @Query("""
        SELECT s FROM NotificationUserState s
        JOIN FETCH s.notification n
        WHERE s.userId = :userId AND s.deletedAt IS NULL
        ORDER BY n.createdAt DESC
        """)
    Page<NotificationUserState> findByUserId(UUID userId, Pageable pageable);

    @Query("""
        SELECT s FROM NotificationUserState s
        WHERE s.userId = :userId AND s.deletedAt IS NULL AND s.readAt IS NULL
        """)
    List<NotificationUserState> findUnreadByUserId(UUID userId);

    Optional<NotificationUserState> findByNotificationIdAndUserId(UUID notificationId, UUID userId);
}
