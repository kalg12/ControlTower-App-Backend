package com.controltower.app.payroll.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriod, UUID> {

    @Query("SELECT p FROM PayrollPeriod p WHERE p.tenantId = :tenantId AND p.deletedAt IS NULL " +
           "ORDER BY p.year DESC, p.periodNumber DESC")
    Page<PayrollPeriod> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    Optional<PayrollPeriod> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
}
