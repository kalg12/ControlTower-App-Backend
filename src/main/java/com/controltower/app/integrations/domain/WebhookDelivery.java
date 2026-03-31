package com.controltower.app.integrations.domain;

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
@Table(name = "webhook_deliveries")
@Getter
@Setter
public class WebhookDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private IntegrationEndpoint endpoint;

    @Column(name = "url", nullable = false)
    private String url;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "JSONB")
    private Map<String, Object> payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "response_status")
    private Integer responseStatus;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    public void recordAttempt(int httpStatus) {
        this.attempts++;
        this.lastAttemptAt  = Instant.now();
        this.responseStatus = httpStatus;
        this.status = (httpStatus >= 200 && httpStatus < 300)
                ? DeliveryStatus.DELIVERED
                : (attempts >= 3 ? DeliveryStatus.FAILED : DeliveryStatus.PENDING);
    }

    public enum DeliveryStatus { PENDING, DELIVERED, FAILED }
}
