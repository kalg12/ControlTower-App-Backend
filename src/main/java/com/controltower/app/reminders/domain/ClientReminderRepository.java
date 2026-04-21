package com.controltower.app.reminders.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ClientReminderRepository extends JpaRepository<ClientReminder, UUID> {

    @Query("SELECT r FROM ClientReminder r WHERE r.tenantId = :tenantId")
    Page<ClientReminder> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT r FROM ClientReminder r WHERE r.tenantId = :tenantId AND r.status = :status")
    Page<ClientReminder> findByTenantAndStatus(
            @Param("tenantId") UUID tenantId,
            @Param("status") ClientReminder.ReminderStatus status, Pageable pageable);

    @Query("SELECT r FROM ClientReminder r WHERE r.tenantId = :tenantId AND r.status = :status")
    List<ClientReminder> findByTenantAndStatusList(
            @Param("tenantId") UUID tenantId,
            @Param("status") ClientReminder.ReminderStatus status);

    @Query("SELECT r FROM ClientReminder r WHERE r.tenantId = :tenantId AND r.clientId = :clientId AND r.status = 'ACTIVE'")
    List<ClientReminder> findActiveByClient(
            @Param("tenantId") UUID tenantId,
            @Param("clientId") UUID clientId);

    @Query("SELECT r FROM ClientReminder r WHERE r.status = 'ACTIVE' AND r.nextDueDate <= :dueDate")
    List<ClientReminder> findDueReminders(@Param("dueDate") Instant dueDate);
}