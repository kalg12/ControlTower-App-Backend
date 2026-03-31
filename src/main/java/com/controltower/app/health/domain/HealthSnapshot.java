package com.controltower.app.health.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Daily health snapshot aggregating uptime, latency, and incident counts for a branch.
 */
@Entity
@Table(
    name = "health_snapshots",
    uniqueConstraints = @UniqueConstraint(columnNames = {"branch_id", "snapshot_date"})
)
@Getter
@Setter
public class HealthSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "uptime_percent")
    private Double uptimePercent;

    @Column(name = "avg_latency_ms")
    private Double avgLatencyMs;

    @Column(name = "check_count", nullable = false)
    private int checkCount;

    @Column(name = "incident_count", nullable = false)
    private int incidentCount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
