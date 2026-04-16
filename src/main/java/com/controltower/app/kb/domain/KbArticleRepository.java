package com.controltower.app.kb.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface KbArticleRepository extends JpaRepository<KbArticle, UUID> {

    Page<KbArticle> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    @Query("""
        SELECT a FROM KbArticle a
        WHERE a.tenantId = :tenantId
          AND a.deletedAt IS NULL
          AND (:status IS NULL OR a.status = :status)
          AND (:category IS NULL OR a.category = :category)
        """)
    Page<KbArticle> findFiltered(
        @Param("tenantId") UUID tenantId,
        @Param("status") KbStatus status,
        @Param("category") String category,
        Pageable pageable);

    @Query("""
        SELECT a FROM KbArticle a
        WHERE a.tenantId = :tenantId
          AND a.deletedAt IS NULL
          AND (LOWER(a.title) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(a.content) LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<KbArticle> searchByText(
        @Param("tenantId") UUID tenantId,
        @Param("q") String q,
        Pageable pageable);

    Optional<KbArticle> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    @Modifying
    @Query("UPDATE KbArticle a SET a.views = a.views + 1 WHERE a.id = :id")
    void incrementViews(@Param("id") UUID id);
}
