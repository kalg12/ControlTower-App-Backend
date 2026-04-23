package com.controltower.app.payroll.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayrollItemRepository extends JpaRepository<PayrollItem, UUID> {

    @Query("SELECT i FROM PayrollItem i JOIN FETCH i.employee e " +
           "WHERE i.period.id = :periodId ORDER BY e.fullName ASC")
    List<PayrollItem> findByPeriodId(@Param("periodId") UUID periodId);

    Optional<PayrollItem> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByPeriodIdAndEmployeeId(UUID periodId, UUID employeeId);
}
