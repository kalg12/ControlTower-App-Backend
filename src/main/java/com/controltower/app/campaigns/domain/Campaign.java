package com.controltower.app.campaigns.domain;

import com.controltower.app.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "campaigns")
@Getter
@Setter
public class Campaign extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private CampaignType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Column(name = "subject")
    private String subject;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "target_audience")
    private String targetAudience;

    @Column(name = "sent_count", nullable = false)
    private int sentCount = 0;

    @Column(name = "open_rate")
    private Double openRate;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    public enum CampaignType {
        EMAIL, SMS, PUSH, IN_APP
    }

    public enum CampaignStatus {
        DRAFT, SCHEDULED, SENT, FAILED, CANCELED
    }
}
