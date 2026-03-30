package com.controltower.app.health.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * A single health check result for a client branch.
 * Created on every heartbeat ping or scheduled pull check.
 */
@Entity
@Table(name = "health_checks")
@Getter
@Setter
public class HealthCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private HealthStatus status = HealthStatus.UNKNOWN;

    /** Round-trip latency in milliseconds (null for heartbeats that timed out). */
    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** Application version reported by the client (e.g., POS version "1.4.2"). */
    @Column(name = "version")
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private CheckSource source = CheckSource.HEARTBEAT;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    public enum HealthStatus {
        UP, DOWN, DEGRADED, UNKNOWN
    }

    public enum CheckSource {
        HEARTBEAT, PULL, WEBHOOK
    }
}
