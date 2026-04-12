package com.controltower.app.clients.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientBranchRepository extends JpaRepository<ClientBranch, UUID> {

    List<ClientBranch> findByClientIdAndDeletedAtIsNull(UUID clientId);

    Optional<ClientBranch> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<ClientBranch> findBySlugAndDeletedAtIsNull(String slug);

    long countByTenantIdAndDeletedAtIsNull(UUID tenantId);

    /** Batch-loads branches with their parent Client eagerly to avoid N+1 in health overview. */
    @Query("""
            SELECT b FROM ClientBranch b
            JOIN FETCH b.client
            WHERE b.id IN :ids
              AND b.deletedAt IS NULL
            """)
    List<ClientBranch> findAllByIdsWithClient(@Param("ids") Collection<UUID> ids);
}
