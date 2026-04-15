package com.controltower.app.time.domain;

import com.controltower.app.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * A single work-log entry linked to a Ticket or Kanban Card.
 * An entry is "active" (timer running) when {@code endedAt} is null.
 */
@Entity
@Table(name = "time_entries")
@Getter
@Setter
public class TimeEntry extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 10)
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    /** Computed minutes between startedAt and endedAt. Set on stop(). */
    @Column(name = "minutes")
    private Integer minutes;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    // ── Domain logic ──────────────────────────────────────────────────

    /** Returns true if the timer is still running (not yet stopped). */
    public boolean isActive() {
        return endedAt == null && getDeletedAt() == null;
    }

    /**
     * Stops the timer: captures endedAt = now and computes elapsed minutes
     * (minimum 1 to avoid zero-minute entries).
     */
    public void stop() {
        if (this.endedAt != null) return; // idempotent
        this.endedAt = Instant.now();
        long computed = Duration.between(this.startedAt, this.endedAt).toMinutes();
        this.minutes = (int) Math.max(computed, 1L);
    }

    public enum EntityType { TICKET, CARD }
}
