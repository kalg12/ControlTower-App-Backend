package com.controltower.app.health.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * An active or resolved health incident for a client branch.
 * Incidents can be opened automatically (by the scheduler detecting consecutive
 * DOWN checks) or manually by a team member.
 */
@Entity
@Table(name = "health_incidents")
@Getter
@Setter
public class HealthIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt = Instant.now();

    /** Null means the incident is still open. */
    @Column(name = "resolved_at")
    private Instant resolvedAt;

    /** User who resolved the incident (null for auto-resolved). */
    @Column(name = "resolved_by")
    private UUID resolvedBy;

    /** Optional note explaining how the incident was resolved. */
    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private Severity severity = Severity.MEDIUM;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** True when opened by the automated health scheduler (not by a human). */
    @Column(name = "auto_created", nullable = false)
    private boolean autoCreated = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean isOpen() {
        return resolvedAt == null;
    }

    public void resolve(UUID resolvedByUserId, String note) {
        this.resolvedAt = Instant.now();
        this.resolvedBy = resolvedByUserId;
        this.resolutionNote = note;
    }

    public void autoResolve() {
        this.resolvedAt = Instant.now();
    }

    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
