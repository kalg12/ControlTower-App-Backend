package com.controltower.app.notifications.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "notification_type"}))
@Getter
@Setter
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "notification_type", nullable = false, length = 100)
    private String notificationType;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;
}
