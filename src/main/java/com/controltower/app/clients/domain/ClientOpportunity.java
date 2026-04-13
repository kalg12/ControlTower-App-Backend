package com.controltower.app.clients.domain;

import com.controltower.app.identity.domain.Tenant;
import com.controltower.app.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Sales opportunity / deal in the pipeline.
 * Tracks potential revenue from prospecting to closed-won/lost.
 */
@Entity
@Table(name = "client_opportunities")
@Getter
@Setter
public class ClientOpportunity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Expected deal value in the tenant's currency. */
    @Column(name = "value")
    private Double value;

    /** Currency code (e.g. MXN, USD). */
    @Column(name = "currency")
    private String currency = "MXN";

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false)
    private OpportunityStage stage;

    /** Probability of closing (percentage 0-100), auto-set by stage. */
    @Column(name = "probability")
    private Integer probability;

    /** User responsible for closing this opportunity. */
    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "owner_name")
    private String ownerName;

    @Column(name = "expected_close_date")
    private Instant expectedCloseDate;

    @Column(name = "closed_date")
    private Instant closedDate;

    /** Why the opportunity was lost (if applicable). */
    @Column(name = "loss_reason")
    private String lossReason;

    /** Source of the opportunity. */
    @Enumerated(EnumType.STRING)
    @Column(name = "source")
    private OpportunitySource source;

    public enum OpportunityStage {
        PROSPECTING(10),
        QUALIFIED(25),
        DEMO_SCHEDULED(40),
        PROPOSAL_SENT(60),
        NEGOTIATION(75),
        VERBAL_COMMIT(90),
        CLOSED_WON(100),
        CLOSED_LOST(0);

        private final int defaultProbability;

        OpportunityStage(int defaultProbability) {
            this.defaultProbability = defaultProbability;
        }

        public int getDefaultProbability() {
            return defaultProbability;
        }
    }

    public enum OpportunitySource {
        INBOUND,
        OUTBOUND,
        REFERRAL,
        WEBSITE,
        EXISTING_CLIENT,
        DEMO_REQUEST,
        SUPPORT_ESCALATION,
        OTHER
    }
}
