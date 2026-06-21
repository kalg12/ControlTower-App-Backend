package com.controltower.app.email.domain;

import com.controltower.app.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "email_routing_rules")
@Getter
@Setter
public class EmailRoutingRule extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "alias_id")
    private UUID aliasId;

    @Column(name = "priority", nullable = false)
    private int priority = 100;

    /**
     * Array of condition objects:
     * [{"field": "subject", "op": "contains", "value": "urgente"}]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditions", columnDefinition = "JSONB", nullable = false)
    private List<Map<String, Object>> conditions;

    /**
     * Array of action objects:
     * [{"type": "set_priority", "value": "HIGH"}]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "actions", columnDefinition = "JSONB", nullable = false)
    private List<Map<String, Object>> actions;

    @Column(name = "match_mode", nullable = false)
    private String matchMode = "ALL";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schedule", columnDefinition = "JSONB")
    private Map<String, Object> schedule;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
