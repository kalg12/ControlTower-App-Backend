package com.controltower.app.audit.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit record. Once created, it is never updated or deleted.
 * Captures who did what, on which resource, when, from where, and with what result.
 */
@Entity
@Table(name = "audit_logs")
@Getter
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private AuditAction action;

    @Column(name = "resource_type")
    private String resourceType;

    @Column(name = "resource_id")
    private String resourceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_value", columnDefinition = "jsonb")
    private String oldValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value", columnDefinition = "jsonb")
    private String newValue;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false)
    private AuditResult result = AuditResult.SUCCESS;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // No setters — constructed via builder only
    protected AuditLog() {}

    private AuditLog(Builder builder) {
        this.tenantId      = builder.tenantId;
        this.userId        = builder.userId;
        this.action        = builder.action;
        this.resourceType  = builder.resourceType;
        this.resourceId    = builder.resourceId;
        this.oldValue      = builder.oldValue;
        this.newValue      = builder.newValue;
        this.ipAddress     = builder.ipAddress;
        this.userAgent     = builder.userAgent;
        this.result        = builder.result != null ? builder.result : AuditResult.SUCCESS;
        this.correlationId = builder.correlationId;
    }

    public static Builder builder(AuditAction action) {
        return new Builder(action);
    }

    public enum AuditResult {
        SUCCESS, FAILURE, PARTIAL
    }

    public static final class Builder {
        private final AuditAction action;
        private UUID tenantId;
        private UUID userId;
        private String resourceType;
        private String resourceId;
        private String oldValue;
        private String newValue;
        private String ipAddress;
        private String userAgent;
        private AuditResult result;
        private String correlationId;

        private Builder(AuditAction action) { this.action = action; }

        public Builder tenantId(UUID v)      { tenantId = v;      return this; }
        public Builder userId(UUID v)        { userId = v;        return this; }
        public Builder resource(String type, String id) {
            resourceType = type; resourceId = id; return this;
        }
        public Builder oldValue(String v)    { oldValue = v;      return this; }
        public Builder newValue(String v)    { newValue = v;      return this; }
        public Builder ipAddress(String v)   { ipAddress = v;     return this; }
        public Builder userAgent(String v)   { userAgent = v;     return this; }
        public Builder result(AuditResult v) { result = v;        return this; }
        public Builder correlationId(String v){ correlationId = v; return this; }
        public AuditLog build()              { return new AuditLog(this); }
    }
}
