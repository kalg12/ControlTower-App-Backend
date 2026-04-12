package com.controltower.app.integrations.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IntegrationEndpointRepository extends JpaRepository<IntegrationEndpoint, UUID> {

    Page<IntegrationEndpoint> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Optional<IntegrationEndpoint> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<IntegrationEndpoint> findByClientBranchIdAndActiveAndDeletedAtIsNull(
            UUID clientBranchId, boolean active);

    /** All active endpoints — used by the pull scheduler. */
    List<IntegrationEndpoint> findByActiveAndDeletedAtIsNullAndPullUrlIsNotNull(boolean active);

    /** Filtered by endpoint type — used by health monitoring UI. */
    Page<IntegrationEndpoint> findByTenantIdAndTypeAndDeletedAtIsNull(
            UUID tenantId, IntegrationEndpoint.EndpointType type, Pageable pageable);
}
