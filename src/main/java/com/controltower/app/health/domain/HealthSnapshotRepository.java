package com.controltower.app.health.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface HealthSnapshotRepository extends JpaRepository<HealthSnapshot, UUID> {

    boolean existsByBranchIdAndSnapshotDate(UUID branchId, LocalDate date);
}
