package com.controltower.app.clients.domain;

import com.controltower.app.identity.domain.Tenant;
import com.controltower.app.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A ClientBranch is a physical or logical location (branch/store/site) of a Client.
 * Health monitoring, tickets, and integrations are scoped to individual branches.
 */
@Entity
@Table(name = "client_branches")
@Getter
@Setter
public class ClientBranch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "country")
    private String country;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    /** URL-friendly identifier used in heartbeat endpoints and integration keys. */
    @Column(name = "slug")
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BranchStatus status = BranchStatus.ACTIVE;

    @Column(name = "timezone", nullable = false)
    private String timezone = "America/Mexico_City";

    public enum BranchStatus {
        ACTIVE, INACTIVE, SUSPENDED
    }
}
