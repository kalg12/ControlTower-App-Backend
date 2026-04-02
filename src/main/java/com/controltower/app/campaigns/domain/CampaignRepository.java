package com.controltower.app.campaigns.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    @Query("""
            SELECT c FROM Campaign c
            WHERE c.tenantId = :tenantId
              AND c.deletedAt IS NULL
              AND (:search IS NULL
                   OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Campaign> findByTenant(
            @Param("tenantId") UUID tenantId,
            @Param("search") String search,
            Pageable pageable);

    Optional<Campaign> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
}
