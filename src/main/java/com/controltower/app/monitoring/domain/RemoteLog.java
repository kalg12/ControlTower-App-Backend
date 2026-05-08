package com.controltower.app.monitoring.domain;

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
@Table(name = "remote_logs", indexes = {
        @Index(name = "idx_remote_logs_tenant_received", columnList = "tenant_id, received_at DESC"),
        @Index(name = "idx_remote_logs_level",           columnList = "level"),
        @Index(name = "idx_remote_logs_service",         columnList = "service_name")
})
@Getter
@Setter
public class RemoteLog {

    public enum Level { DEBUG, INFO, WARN, ERROR, CRITICAL }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "endpoint_id")
    private UUID endpointId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Level level;

    @Column(name = "service_name")
    private String serviceName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "business_name")
    private String businessName;

    @Column(length = 100)
    private String source = "POS";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;
}
