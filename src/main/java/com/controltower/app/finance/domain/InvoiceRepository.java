package com.controltower.app.finance.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
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

    @Query("""
        SELECT COALESCE(SUM(i.total), 0) FROM Invoice i
        WHERE i.clientId = :clientId AND i.deletedAt IS NULL
        """)
    java.math.BigDecimal sumTotalByClientId(@Param("clientId") UUID clientId);

    @Query("""
        SELECT COALESCE(SUM(i.total), 0) FROM Invoice i
        WHERE i.clientId = :clientId AND i.deletedAt IS NULL
          AND i.status = com.controltower.app.finance.domain.Invoice.InvoiceStatus.PAID
        """)
    java.math.BigDecimal sumPaidByClientId(@Param("clientId") UUID clientId);

    long countByClientIdAndDeletedAtIsNull(UUID clientId);

    @Query("""
        SELECT MAX(i.createdAt) FROM Invoice i
        WHERE i.clientId = :clientId AND i.deletedAt IS NULL
        """)
    java.time.Instant findLastInvoiceAtByClientId(@Param("clientId") UUID clientId);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.tenantId = :tenantId AND i.deletedAt IS NULL AND FUNCTION('date_part', 'year', i.createdAt) = :year")
    long countByTenantIdAndYear(@Param("tenantId") UUID tenantId, @Param("year") int year);

    @Query("""
        SELECT i FROM Invoice i
        WHERE i.deletedAt IS NULL
          AND i.status = com.controltower.app.finance.domain.Invoice.InvoiceStatus.SENT
          AND i.dueDate < :today
        """)
    List<Invoice> findOverdueInvoices(@Param("today") java.time.LocalDate today);

    @Query("""
        SELECT i FROM Invoice i
        WHERE i.deletedAt IS NULL
          AND i.status = com.controltower.app.finance.domain.Invoice.InvoiceStatus.SENT
          AND i.dueDate = :dueDate
        """)
    List<Invoice> findInvoicesDueOn(@Param("dueDate") java.time.LocalDate dueDate);

    @Query("""
        SELECT i FROM Invoice i
        WHERE i.isRecurring = true
          AND i.deletedAt IS NULL
          AND i.nextOccurrenceDate <= :today
          AND (i.recurrenceEndDate IS NULL OR i.recurrenceEndDate >= :today)
        """)
    List<Invoice> findDueForRecurrence(@Param("today") LocalDate today);
}
