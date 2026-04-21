package com.controltower.app.reminders.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "client_reminders")
@Getter
@Setter
public class ClientReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", nullable = false)
    private RecurrenceType recurrenceType = RecurrenceType.WEEKLY;

    @Column(name = "recurrence_days")
    private Integer recurrenceDays;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "next_due_date", nullable = false)
    private Instant nextDueDate;

    @Column(name = "last_completed_date")
    private Instant lastCompletedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReminderStatus status = ReminderStatus.ACTIVE;

    @Column(name = "max_occurrences")
    private Integer maxOccurrences;

    @Column(name = "occurrences_count")
    private Integer occurrencesCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "notify_user_ids", columnDefinition = "UUID[]")
    private List<UUID> notifyUserIds;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum RecurrenceType {
        DAILY, WEEKLY, BIWEEKLY, MONTHLY, CUSTOM
    }

    public enum ReminderStatus {
        ACTIVE, PAUSED, COMPLETED
    }

    @PrePersist
    public void prePersist() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public void calculateNextDueDate() {
        Instant base = lastCompletedDate != null ? lastCompletedDate : Instant.now();
        Instant next;
        
        switch (recurrenceType) {
            case DAILY -> next = base.plusSeconds(86400L);
            case WEEKLY -> next = base.plusSeconds(7 * 86400L);
            case BIWEEKLY -> next = base.plusSeconds(14 * 86400L);
            case MONTHLY -> next = base.plusSeconds(30L * 86400L);
            case CUSTOM -> next = base.plusSeconds((long) (recurrenceDays != null ? recurrenceDays : 7) * 86400L);
            default -> next = base.plusSeconds(7 * 86400L);
        }
        
        this.nextDueDate = next;
        this.occurrencesCount = (occurrencesCount != null ? occurrencesCount : 0) + 1;
    }
}