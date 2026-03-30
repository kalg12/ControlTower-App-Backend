package com.controltower.app.health.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A rule that defines when to open a health incident and at what severity.
 * Example: "open CRITICAL incident if branch_id X has 3 consecutive DOWN checks"
 */
@Entity
@Table(name = "health_rules")
@Getter
@Setter
public class HealthRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Null means the rule applies to all branches in the tenant. */
    @Column(name = "branch_id")
    private UUID branchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    private RuleType ruleType;

    /**
     * Meaning depends on ruleType:
     *   CONSECUTIVE_DOWN_CHECKS: number of consecutive DOWN checks before opening incident
     *   LATENCY_THRESHOLD_MS:    latency in ms above which status becomes DEGRADED
     *   MISSED_HEARTBEATS:       number of missed heartbeat windows before DOWN
     */
    @Column(name = "threshold_value")
    private Integer thresholdValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private HealthIncident.Severity severity = HealthIncident.Severity.MEDIUM;

    @Column(name = "alert_channel")
    private String alertChannel;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public enum RuleType {
        CONSECUTIVE_DOWN_CHECKS,
        LATENCY_THRESHOLD_MS,
        MISSED_HEARTBEATS
    }
}
