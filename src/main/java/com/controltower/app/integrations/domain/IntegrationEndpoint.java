package com.controltower.app.integrations.domain;

import com.controltower.app.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "integration_endpoints")
@Getter
@Setter
public class IntegrationEndpoint extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "client_branch_id")
    private UUID clientBranchId;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private EndpointType type = EndpointType.CUSTOM;

    @Column(name = "pull_url")
    private String pullUrl;

    /** Stored encrypted in production — plain text here for simplicity. */
    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "heartbeat_interval_seconds", nullable = false)
    private int heartbeatIntervalSeconds = 300;

    @Column(name = "contract_version")
    private String contractVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSONB")
    private Map<String, Object> metadata;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public enum EndpointType { POS, CUSTOM }
}
