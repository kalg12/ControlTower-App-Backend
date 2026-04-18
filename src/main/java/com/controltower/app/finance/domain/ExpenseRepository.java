package com.controltower.app.finance.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    Optional<Expense> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
        SELECT e FROM Expense e
        WHERE e.tenantId = :tenantId
          AND e.deletedAt IS NULL
          AND (:category IS NULL OR e.category = :category)
          AND (:clientId IS NULL OR e.clientId = :clientId)
        ORDER BY e.paidAt DESC
        """)
    Page<Expense> findFiltered(
            @Param("tenantId") UUID tenantId,
            @Param("category") Expense.ExpenseCategory category,
            @Param("clientId") UUID clientId,
            Pageable pageable);

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0) FROM Expense e
        WHERE e.clientId = :clientId AND e.deletedAt IS NULL
        """)
    java.math.BigDecimal sumAmountByClientId(@Param("clientId") UUID clientId);

    long countByClientIdAndDeletedAtIsNull(UUID clientId);

    @Query("""
        SELECT e FROM Expense e
        WHERE e.tenantId = :tenantId
          AND e.deletedAt IS NULL
          AND e.paidAt >= :from
          AND e.paidAt < :to
        """)
    List<Expense> findByTenantIdAndPaidAtBetween(
            @Param("tenantId") UUID tenantId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
