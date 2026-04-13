package com.controltower.app.clients.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientInteractionRepository extends JpaRepository<ClientInteraction, UUID> {

    @Query("SELECT ci FROM ClientInteraction ci WHERE ci.client.id = :clientId AND ci.tenant.id = :tenantId AND ci.deletedAt IS NULL ORDER BY ci.occurredAt DESC")
    Page<ClientInteraction> findByClientIdAndTenantIdOrderByOccurredAtDesc(
            @Param("clientId") UUID clientId,
            @Param("tenantId") UUID tenantId,
            Pageable pageable);

    @Query("SELECT ci FROM ClientInteraction ci WHERE ci.client.id = :clientId AND ci.tenant.id = :tenantId AND ci.deletedAt IS NULL ORDER BY ci.occurredAt DESC")
    List<ClientInteraction> findByClientIdAndTenantIdOrderByOccurredAtDesc(
            @Param("clientId") UUID clientId,
            @Param("tenantId") UUID tenantId);

    @Query("SELECT ci FROM ClientInteraction ci WHERE ci.id = :id AND ci.tenant.id = :tenantId AND ci.deletedAt IS NULL")
    Optional<ClientInteraction> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT ci FROM ClientInteraction ci WHERE ci.userId = :userId AND ci.tenant.id = :tenantId AND ci.deletedAt IS NULL ORDER BY ci.occurredAt DESC")
    Page<ClientInteraction> findByUserIdAndTenantIdOrderByOccurredAtDesc(
            @Param("userId") UUID userId,
            @Param("tenantId") UUID tenantId,
            Pageable pageable);

    long countByClientIdAndTenantIdAndDeletedAtIsNull(UUID clientId, UUID tenantId);
}
