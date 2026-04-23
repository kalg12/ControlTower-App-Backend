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
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Optional<Employee> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    @Query("SELECT e FROM Employee e WHERE e.tenantId = :tenantId AND e.deletedAt IS NULL " +
           "AND (:status IS NULL OR e.status = :status)")
    Page<Employee> findByTenantId(@Param("tenantId") UUID tenantId,
                                  @Param("status") Employee.EmployeeStatus status,
                                  Pageable pageable);

    boolean existsByTenantIdAndRfcAndDeletedAtIsNull(UUID tenantId, String rfc);

    boolean existsByTenantIdAndRfcAndIdNotAndDeletedAtIsNull(UUID tenantId, String rfc, UUID id);
}
