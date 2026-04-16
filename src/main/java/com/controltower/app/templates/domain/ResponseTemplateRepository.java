package com.controltower.app.templates.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ResponseTemplateRepository extends JpaRepository<ResponseTemplate, UUID> {

    Page<ResponseTemplate> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    @Query("""
        SELECT t FROM ResponseTemplate t
        WHERE t.tenantId = :tenantId
          AND t.deletedAt IS NULL
          AND (:category IS NULL OR t.category = :category)
          AND (:q IS NULL
               OR LOWER(t.name) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(t.body) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(t.shortcut) LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<ResponseTemplate> findFiltered(
        @Param("tenantId") UUID tenantId,
        @Param("category") String category,
        @Param("q") String q,
        Pageable pageable);

    Optional<ResponseTemplate> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
}
