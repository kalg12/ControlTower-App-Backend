package com.controltower.app.finance.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByIdAndDeletedAtIsNull(UUID id);

    Page<Invoice> findAllByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    @Query("""
        SELECT i FROM Invoice i
        WHERE i.tenantId = :tenantId
          AND i.deletedAt IS NULL
          AND (:status IS NULL OR i.status = :status)
          AND (:clientId IS NULL OR i.clientId = :clientId)
        ORDER BY i.createdAt DESC
        """)
    Page<Invoice> findFiltered(
            @Param("tenantId") UUID tenantId,
            @Param("status") Invoice.InvoiceStatus status,
            @Param("clientId") UUID clientId,
            Pageable pageable);

    boolean existsByTenantIdAndNumber(UUID tenantId, String number);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.tenantId = :tenantId AND i.deletedAt IS NULL AND FUNCTION('date_part', 'year', i.createdAt) = :year")
    long countByTenantIdAndYear(@Param("tenantId") UUID tenantId, @Param("year") int year);
}
