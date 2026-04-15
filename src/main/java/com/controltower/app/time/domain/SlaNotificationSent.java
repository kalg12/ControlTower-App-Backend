package com.controltower.app.time.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Deduplication record so SLA warning notifications are sent only once per threshold.
 * Thresholds: 50%, 75%, 90% of SLA window consumed.
 */
@Entity
@Table(
    name = "sla_notifications_sent",
    uniqueConstraints = @UniqueConstraint(columnNames = {"ticket_id", "threshold"})
)
@Getter
@Setter
public class SlaNotificationSent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "threshold", nullable = false)
    private short threshold;

    @CreationTimestamp
    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;
}
