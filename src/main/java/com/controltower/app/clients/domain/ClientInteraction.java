package com.controltower.app.clients.domain;

import com.controltower.app.identity.domain.Tenant;
import com.controltower.app.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an interaction with a client — calls, meetings, emails, messages.
 * Forms the activity log for CRM timeline.
 */
@Entity
@Table(name = "client_interactions")
@Getter
@Setter
public class ClientInteraction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "branch_id")
    private UUID branchId;

    /** The user who performed this interaction (Control Tower operator). */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_name")
    private String userName;

    /** Type of interaction. */
    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type", nullable = false)
    private InteractionType interactionType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** When the interaction actually occurred (may differ from createdAt). */
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /** Optional link to a related ticket. */
    @Column(name = "ticket_id")
    private UUID ticketId;

    /** Outcome or follow-up notes. */
    @Column(name = "outcome")
    private String outcome;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    public enum InteractionType {
        CALL,
        MEETING,
        EMAIL,
        MESSAGE,
        SITE_VISIT,
        DEMO,
        SUPPORT,
        FOLLOW_UP,
        OTHER
    }
}
