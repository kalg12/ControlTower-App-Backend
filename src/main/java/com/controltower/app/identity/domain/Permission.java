package com.controltower.app.identity.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * A granular permission that can be assigned to roles.
 * Permissions are system-wide (not tenant-scoped) and seeded in V2 migration.
 */
@Entity
@Table(name = "permissions")
@Getter
@Setter
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Machine-readable code (e.g., "ticket:write", "license:read"). */
    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "description")
    private String description;

    /** The module this permission belongs to (e.g., "support", "licenses"). */
    @Column(name = "module", nullable = false)
    private String module;
}
