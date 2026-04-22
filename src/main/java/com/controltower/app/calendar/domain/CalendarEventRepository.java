package com.controltower.app.calendar.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {

    @Query("SELECT e FROM CalendarEvent e WHERE e.tenant.id = :tenantId AND e.deletedAt IS NULL " +
           "AND e.startAt >= :rangeFrom AND e.startAt < :rangeTo ORDER BY e.startAt")
    List<CalendarEvent> findByTenantAndRange(
            @Param("tenantId") UUID tenantId,
            @Param("rangeFrom") Instant rangeFrom,
            @Param("rangeTo") Instant rangeTo);

    @Query("SELECT e FROM CalendarEvent e WHERE e.tenant.id = :tenantId AND e.clientId = :clientId " +
           "AND e.deletedAt IS NULL AND e.startAt >= :after ORDER BY e.startAt")
    List<CalendarEvent> findByTenantAndClientAfter(
            @Param("tenantId") UUID tenantId,
            @Param("clientId") UUID clientId,
            @Param("after") Instant after);

    @Query("SELECT e FROM CalendarEvent e WHERE e.tenant.id = :tenantId AND e.personId = :personId " +
           "AND e.deletedAt IS NULL AND e.startAt >= :after ORDER BY e.startAt")
    List<CalendarEvent> findByTenantAndPersonAfter(
            @Param("tenantId") UUID tenantId,
            @Param("personId") UUID personId,
            @Param("after")    Instant after);

    @Query("SELECT e FROM CalendarEvent e WHERE e.id = :id AND e.tenant.id = :tenantId AND e.deletedAt IS NULL")
    Optional<CalendarEvent> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
}
