package com.controltower.app.billing.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "stripe_webhook_events")
@Getter
@Setter
public class StripeWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Stripe event ID — used for idempotency. */
    @Column(name = "stripe_event_id", nullable = false, unique = true)
    private String stripeEventId;

    @Column(name = "type", nullable = false)
    private String type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "JSONB")
    private Map<String, Object> payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WebhookStatus status = WebhookStatus.RECEIVED;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "received_at", updatable = false, nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    public void markProcessed() {
        this.status      = WebhookStatus.PROCESSED;
        this.processedAt = Instant.now();
    }

    public void markFailed(String error) {
        this.status       = WebhookStatus.FAILED;
        this.errorMessage = error;
    }

    public void markSkipped() {
        this.status      = WebhookStatus.SKIPPED;
        this.processedAt = Instant.now();
    }

    public enum WebhookStatus { RECEIVED, PROCESSED, FAILED, SKIPPED }
}
