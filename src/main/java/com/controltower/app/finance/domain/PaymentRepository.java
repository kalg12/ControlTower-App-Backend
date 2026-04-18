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
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
        SELECT p FROM Payment p
        WHERE p.tenantId = :tenantId
          AND p.deletedAt IS NULL
          AND (:clientId IS NULL OR p.clientId = :clientId)
        ORDER BY p.paidAt DESC
        """)
    Page<Payment> findFiltered(
            @Param("tenantId") UUID tenantId,
            @Param("clientId") UUID clientId,
            Pageable pageable);

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0) FROM Payment p
        WHERE p.clientId = :clientId AND p.deletedAt IS NULL
        """)
    java.math.BigDecimal sumAmountByClientId(@Param("clientId") UUID clientId);

    long countByClientIdAndDeletedAtIsNull(UUID clientId);

    @Query("""
        SELECT p FROM Payment p
        WHERE p.tenantId = :tenantId
          AND p.deletedAt IS NULL
          AND p.paidAt >= :from
          AND p.paidAt < :to
        """)
    List<Payment> findByTenantIdAndPaidAtBetween(
            @Param("tenantId") UUID tenantId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
