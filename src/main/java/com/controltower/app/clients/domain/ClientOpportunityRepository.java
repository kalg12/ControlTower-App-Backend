package com.controltower.app.clients.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientOpportunityRepository extends JpaRepository<ClientOpportunity, UUID> {

    @Query("SELECT co FROM ClientOpportunity co WHERE co.client.id = :clientId AND co.tenant.id = :tenantId AND co.deletedAt IS NULL ORDER BY co.createdAt DESC")
    Page<ClientOpportunity> findByClientIdAndTenantIdOrderByCreatedAtDesc(
            @Param("clientId") UUID clientId,
            @Param("tenantId") UUID tenantId,
            Pageable pageable);

    @Query("SELECT co FROM ClientOpportunity co WHERE co.tenant.id = :tenantId AND co.deletedAt IS NULL ORDER BY co.createdAt DESC")
    Page<ClientOpportunity> findByTenantIdOrderByCreatedAtDesc(
            @Param("tenantId") UUID tenantId,
            Pageable pageable);

    @Query("SELECT co FROM ClientOpportunity co WHERE co.id = :id AND co.tenant.id = :tenantId AND co.deletedAt IS NULL")
    Optional<ClientOpportunity> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT co FROM ClientOpportunity co WHERE co.stage IN :stages AND co.tenant.id = :tenantId AND co.deletedAt IS NULL ORDER BY co.value DESC")
    List<ClientOpportunity> findActivePipelineByTenantId(
            @Param("stages") List<ClientOpportunity.OpportunityStage> stages,
            @Param("tenantId") UUID tenantId);

    @Query("SELECT co FROM ClientOpportunity co WHERE co.ownerId = :ownerId AND co.tenant.id = :tenantId AND co.deletedAt IS NULL ORDER BY co.expectedCloseDate ASC")
    List<ClientOpportunity> findByOwnerIdAndTenantIdOrderByExpectedCloseDateAsc(
            @Param("ownerId") UUID ownerId,
            @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(co) FROM ClientOpportunity co WHERE co.client.id = :clientId AND co.tenant.id = :tenantId AND co.deletedAt IS NULL AND co.stage NOT IN ('CLOSED_WON', 'CLOSED_LOST')")
    long countOpenOpportunities(@Param("clientId") UUID clientId, @Param("tenantId") UUID tenantId);

    @Query("SELECT SUM(co.value) FROM ClientOpportunity co WHERE co.client.id = :clientId AND co.tenant.id = :tenantId AND co.deletedAt IS NULL AND co.stage = 'CLOSED_WON'")
    Double sumWonValueByClient(@Param("clientId") UUID clientId, @Param("tenantId") UUID tenantId);
}
