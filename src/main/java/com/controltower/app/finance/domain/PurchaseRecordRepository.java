package com.controltower.app.finance.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseRecordRepository extends JpaRepository<PurchaseRecord, UUID> {

    Optional<PurchaseRecord> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
        SELECT p FROM PurchaseRecord p
        WHERE p.tenantId = :tenantId
          AND p.deletedAt IS NULL
          AND (:source IS NULL OR p.source = :source)
          AND (:category IS NULL OR p.category = :category)
          AND (:vendor IS NULL OR LOWER(p.vendor) LIKE LOWER(CONCAT('%', :vendor, '%')))
          AND (:from IS NULL OR p.purchasedAt >= :from)
          AND (:to IS NULL OR p.purchasedAt <= :to)
        ORDER BY p.purchasedAt DESC
        """)
    Page<PurchaseRecord> findFiltered(
            @Param("tenantId") UUID tenantId,
            @Param("source") PurchaseRecord.PurchaseSource source,
            @Param("category") Expense.ExpenseCategory category,
            @Param("vendor") String vendor,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @Query("""
        SELECT p FROM PurchaseRecord p
        WHERE p.tenantId = :tenantId
          AND p.deletedAt IS NULL
          AND p.purchasedAt >= :from
          AND p.purchasedAt < :to
        """)
    List<PurchaseRecord> findByTenantIdAndPurchasedAtBetween(
            @Param("tenantId") UUID tenantId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("""
        SELECT p FROM PurchaseRecord p
        WHERE p.isRecurring = true
          AND p.deletedAt IS NULL
          AND p.nextOccurrenceDate <= :today
          AND (p.recurrenceEndDate IS NULL OR p.recurrenceEndDate >= :today)
        """)
    List<PurchaseRecord> findDueForRecurrence(@Param("today") LocalDate today);
}
