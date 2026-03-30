package com.controltower.app.tenancy.domain;

import com.controltower.app.identity.domain.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Key-value configuration store scoped to a tenant.
 * Allows per-tenant customization without schema changes.
 */
@Entity
@Table(
    name = "tenant_configs",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "key"})
)
@Getter
@Setter
public class TenantConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "key", nullable = false)
    private String key;

    @Column(name = "value", columnDefinition = "TEXT")
    private String value;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
