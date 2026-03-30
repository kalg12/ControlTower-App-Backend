package com.controltower.app.identity.domain;

import com.controltower.app.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * A role groups permissions and is assigned to users within a tenant.
 * System roles (isSystem = true) are protected from deletion.
 */
@Entity
@Table(
    name = "roles",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "code"})
)
@Getter
@Setter
public class Role extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(name = "name", nullable = false)
    private String name;

    /** Unique code within a tenant (e.g., "ADMIN", "SUPPORT_AGENT"). */
    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "description")
    private String description;

    /** System roles cannot be deleted or modified by tenants. */
    @Column(name = "is_system", nullable = false)
    private boolean system = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();
}
