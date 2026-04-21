package com.controltower.app.calendar.application;

import com.controltower.app.calendar.api.dto.CalendarEventRequest;
import com.controltower.app.calendar.api.dto.CalendarEventResponse;
import com.controltower.app.calendar.domain.CalendarEvent;
import com.controltower.app.calendar.domain.CalendarEventRepository;
import com.controltower.app.clients.domain.ClientRepository;
import com.controltower.app.identity.domain.Tenant;
import com.controltower.app.identity.domain.TenantRepository;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarEventRepository eventRepository;
    private final TenantRepository        tenantRepository;
    private final ClientRepository        clientRepository;

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> listEvents(Instant from, Instant to, UUID clientId, UUID assigneeId) {
        UUID tenantId = TenantContext.getTenantId();
        List<CalendarEvent> events;

        if (clientId != null) {
            events = eventRepository.findByTenantAndClientAfter(tenantId, clientId, from);
        } else {
            events = eventRepository.findByTenantAndRange(tenantId, from, to);
        }

        return events.stream()
                .filter(e -> assigneeId == null || e.getAssigneeIds().contains(assigneeId))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CalendarEventResponse createEvent(CalendarEventRequest req, UUID userId) {
        UUID tenantId = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        CalendarEvent event = new CalendarEvent();
        event.setTenant(tenant);
        event.setCreatedBy(userId);
        applyRequest(event, req);

        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public CalendarEventResponse updateEvent(UUID id, CalendarEventRequest req) {
        UUID tenantId = TenantContext.getTenantId();
        CalendarEvent event = eventRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("CalendarEvent", id));
        applyRequest(event, req);
        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public CalendarEventResponse patchStatus(UUID id, CalendarEvent.EventStatus status) {
        UUID tenantId = TenantContext.getTenantId();
        CalendarEvent event = eventRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("CalendarEvent", id));
        event.setStatus(status);
        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public void deleteEvent(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        CalendarEvent event = eventRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("CalendarEvent", id));
        event.softDelete();
        eventRepository.save(event);
    }

    private void applyRequest(CalendarEvent event, CalendarEventRequest req) {
        event.setTitle(req.getTitle());
        event.setDescription(req.getDescription());
        event.setEventType(req.getEventType());
        event.setStartAt(req.getStartAt());
        event.setEndAt(req.getEndAt());
        event.setClientId(req.getClientId());
        event.setBranchId(req.getBranchId());
        event.setNotes(req.getNotes());
        event.setOutcome(req.getOutcome());
        event.setContactChannel(req.getContactChannel());
        if (req.getStatus() != null) event.setStatus(req.getStatus());
        if (req.getAssigneeIds() != null) event.setAssigneeIds(req.getAssigneeIds());
    }

    private CalendarEventResponse toResponse(CalendarEvent e) {
        String clientName = null;
        if (e.getClientId() != null) {
            clientName = clientRepository.findById(e.getClientId())
                    .map(c -> c.getName()).orElse(null);
        }
        return CalendarEventResponse.builder()
                .id(e.getId())
                .tenantId(e.getTenant().getId())
                .title(e.getTitle())
                .description(e.getDescription())
                .eventType(e.getEventType())
                .startAt(e.getStartAt())
                .endAt(e.getEndAt())
                .clientId(e.getClientId())
                .clientName(clientName)
                .branchId(e.getBranchId())
                .status(e.getStatus())
                .notes(e.getNotes())
                .outcome(e.getOutcome())
                .contactChannel(e.getContactChannel())
                .createdBy(e.getCreatedBy())
                .assigneeIds(e.getAssigneeIds())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
