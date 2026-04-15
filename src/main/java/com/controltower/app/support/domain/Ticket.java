package com.controltower.app.support.domain;

import com.controltower.app.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "tickets")
@Getter
@Setter
public class Ticket extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TicketStatus status = TicketStatus.OPEN;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private TicketSource source = TicketSource.MANUAL;

    /** ID of the originating resource (e.g., health incident UUID). */
    @Column(name = "source_ref_id")
    private String sourceRefId;

    /** POS-specific context (branch, submitter, category, etc.). Populated when source=POS. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pos_context", columnDefinition = "JSONB")
    private Map<String, Object> posContext;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Array(length = 20)
    @Column(name = "labels", columnDefinition = "TEXT[]")
    private String[] labels = new String[0];

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TicketComment> comments = new ArrayList<>();

    @OneToOne(mappedBy = "ticket", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private TicketSla sla;

    // ── State machine ─────────────────────────────────────────────────

    public void assign(UUID userId) {
        this.assigneeId = userId;
        if (this.status == TicketStatus.OPEN) {
            this.status = TicketStatus.IN_PROGRESS;
        }
    }

    public void escalate() {
        if (this.priority != Priority.CRITICAL) {
            Priority[] values = Priority.values();
            this.priority = values[Math.min(this.priority.ordinal() + 1, values.length - 1)];
        }
    }

    public void resolve() {
        this.status = TicketStatus.RESOLVED;
    }

    public void close() {
        this.status = TicketStatus.CLOSED;
    }

    public void reopen() {
        this.status = TicketStatus.OPEN;
    }

    public enum Priority   { LOW, MEDIUM, HIGH, CRITICAL }
    public enum TicketStatus { OPEN, IN_PROGRESS, WAITING, RESOLVED, CLOSED }
    public enum TicketSource { MANUAL, HEALTH_ALERT, WEBHOOK, EMAIL, POS }
}
