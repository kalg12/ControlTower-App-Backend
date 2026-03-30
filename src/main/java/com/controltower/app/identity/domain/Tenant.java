package com.controltower.app.identity.domain;

import com.controltower.app.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * A Tenant is a Control Tower operator organization.
 * Each tenant is isolated: their users, clients, and data are scoped to tenant_id.
 */
@Entity
@Table(name = "tenants")
@Getter
@Setter
public class Tenant extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    /** URL-friendly identifier used in slugs and references. */
    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TenantStatus status = TenantStatus.ACTIVE;

    @OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();

    public enum TenantStatus {
        ACTIVE, SUSPENDED, CANCELLED
    }
}
