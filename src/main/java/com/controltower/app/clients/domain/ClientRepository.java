package com.controltower.app.clients.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {

    Page<Client> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Optional<Client> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    @Query("""
        SELECT c FROM Client c
        WHERE c.tenant.id = :tenantId
          AND c.deletedAt IS NULL
          AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(c.legalName) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<Client> searchByTenant(UUID tenantId, String search, Pageable pageable);
}
