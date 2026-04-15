package com.controltower.app.time.application;

import com.controltower.app.kanban.domain.Card;
import com.controltower.app.kanban.domain.CardRepository;
import com.controltower.app.shared.exception.ControlTowerException;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.support.domain.Ticket;
import com.controltower.app.support.domain.TicketRepository;
import com.controltower.app.tenancy.domain.TenantContext;
import com.controltower.app.time.api.dto.*;
import com.controltower.app.time.domain.TimeEntry;
import com.controltower.app.time.domain.TimeEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TimeEntryService {

    private final TimeEntryRepository timeEntryRepository;
    private final TicketRepository    ticketRepository;
    private final CardRepository      cardRepository;

    // ── Start timer ───────────────────────────────────────────────────

    /**
     * Starts a new timer for the authenticated user on the given entity.
     * If the user already has an active timer, it is stopped first (one active per user).
     */
    @Transactional
    public TimeEntryResponse startTimer(StartTimerRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId   = resolveUserId();

        validateEntityExists(request.getEntityType(), request.getEntityId(), tenantId);

        // Auto-stop any running timer for this user
        timeEntryRepository.findActiveByTenantAndUser(tenantId, userId)
                .ifPresent(active -> {
                    active.stop();
                    timeEntryRepository.save(active);
                });

        TimeEntry entry = new TimeEntry();
        entry.setTenantId(tenantId);
        entry.setUserId(userId);
        entry.setEntityType(request.getEntityType());
        entry.setEntityId(request.getEntityId());
        entry.setStartedAt(Instant.now());

        return toResponse(timeEntryRepository.save(entry));
    }

    // ── Stop timer ────────────────────────────────────────────────────

    @Transactional
    public TimeEntryResponse stopTimer(UUID entryId) {
        TimeEntry entry = resolveEntry(entryId);
        if (!entry.isActive()) {
            throw new ControlTowerException("Timer is not running", HttpStatus.BAD_REQUEST);
        }
        entry.stop();
        return toResponse(timeEntryRepository.save(entry));
    }

    // ── Get active timer ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<TimeEntryResponse> getActiveTimer() {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId   = resolveUserId();
        return timeEntryRepository.findActiveByTenantAndUser(tenantId, userId)
                .map(this::toResponse);
    }

    // ── Manual log ────────────────────────────────────────────────────

    @Transactional
    public TimeEntryResponse logManual(LogTimeRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId   = resolveUserId();

        validateEntityExists(request.getEntityType(), request.getEntityId(), tenantId);

        TimeEntry entry = new TimeEntry();
        entry.setTenantId(tenantId);
        entry.setUserId(userId);
        entry.setEntityType(request.getEntityType());
        entry.setEntityId(request.getEntityId());
        entry.setStartedAt(Instant.now().minusSeconds((long) request.getMinutes() * 60));
        entry.setEndedAt(Instant.now());
        entry.setMinutes(request.getMinutes());
        entry.setNote(request.getNote());

        return toResponse(timeEntryRepository.save(entry));
    }

    // ── Delete entry ──────────────────────────────────────────────────

    @Transactional
    public void deleteEntry(UUID entryId) {
        TimeEntry entry = resolveEntry(entryId);
        entry.softDelete();
        timeEntryRepository.save(entry);
    }

    // ── List entries ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TimeEntryResponse> listEntries(TimeEntry.EntityType entityType, UUID entityId) {
        return timeEntryRepository.findByEntityTypeAndEntityId(entityType, entityId)
                .stream().map(this::toResponse).toList();
    }

    // ── Time summary ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TimeSummaryResponse getSummary(TimeEntry.EntityType entityType, UUID entityId) {
        Integer estimatedMinutes = fetchEstimatedMinutes(entityType, entityId);
        int loggedMinutes = timeEntryRepository.sumMinutesByEntityTypeAndEntityId(entityType, entityId);
        List<TimeEntryResponse> entries = listEntries(entityType, entityId);
        return TimeSummaryResponse.builder()
                .entityId(entityId)
                .entityType(entityType)
                .estimatedMinutes(estimatedMinutes)
                .loggedMinutes(loggedMinutes)
                .entries(entries)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private TimeEntry resolveEntry(UUID entryId) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId   = resolveUserId();
        TimeEntry entry = timeEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("TimeEntry", entryId));
        if (!entry.getTenantId().equals(tenantId) || !entry.getUserId().equals(userId)) {
            throw new ControlTowerException("Access denied", HttpStatus.FORBIDDEN);
        }
        if (entry.isDeleted()) {
            throw new ResourceNotFoundException("TimeEntry", entryId);
        }
        return entry;
    }

    private void validateEntityExists(TimeEntry.EntityType type, UUID entityId, UUID tenantId) {
        boolean exists = switch (type) {
            case TICKET -> ticketRepository.findByIdAndTenantIdAndDeletedAtIsNull(entityId, tenantId).isPresent();
            case CARD   -> cardRepository.findById(entityId)
                    .map(c -> c.getDeletedAt() == null)
                    .orElse(false);
        };
        if (!exists) {
            throw new ResourceNotFoundException(type.name(), entityId);
        }
    }

    private Integer fetchEstimatedMinutes(TimeEntry.EntityType type, UUID entityId) {
        return switch (type) {
            case TICKET -> ticketRepository.findById(entityId)
                    .map(Ticket::getEstimatedMinutes).orElse(null);
            case CARD   -> cardRepository.findById(entityId)
                    .map(Card::getEstimatedMinutes).orElse(null);
        };
    }

    private UUID resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new ControlTowerException("Not authenticated", HttpStatus.UNAUTHORIZED);
        }
        // Principal is the user UUID string set by JwtAuthenticationFilter
        return UUID.fromString(auth.getName());
    }

    public TimeEntryResponse toResponse(TimeEntry e) {
        return TimeEntryResponse.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .entityType(e.getEntityType())
                .entityId(e.getEntityId())
                .startedAt(e.getStartedAt())
                .endedAt(e.getEndedAt())
                .minutes(e.getMinutes())
                .note(e.getNote())
                .active(e.isActive())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
