package com.controltower.app.calendar.application;

import com.controltower.app.calendar.api.dto.CalendarEventRequest;
import com.controltower.app.calendar.api.dto.CalendarEventResponse;
import com.controltower.app.calendar.domain.CalendarEvent;
import com.controltower.app.calendar.domain.CalendarEventRepository;
import com.controltower.app.clients.domain.ClientRepository;
import com.controltower.app.identity.domain.Tenant;
import com.controltower.app.identity.domain.TenantRepository;
import com.controltower.app.identity.domain.UserRepository;
import com.controltower.app.notifications.application.NotificationService;
import com.controltower.app.notifications.domain.Notification;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarEventRepository eventRepository;
    private final TenantRepository        tenantRepository;
    private final ClientRepository        clientRepository;
    private final UserRepository        userRepository;
    private final NotificationService  notificationService;

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

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

        CalendarEvent saved = eventRepository.save(event);
        notifyAssigneesOnCreate(saved);
        
        return toResponse(saved);
    }

    @Transactional
    public CalendarEventResponse updateEvent(UUID id, CalendarEventRequest req) {
        UUID tenantId = TenantContext.getTenantId();
        CalendarEvent event = eventRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("CalendarEvent", id));
        
        List<UUID> oldAssignees = new ArrayList<>(event.getAssigneeIds() != null ? event.getAssigneeIds() : Collections.emptyList());
        Instant oldStartAt = event.getStartAt();
        String oldTitle = event.getTitle();
        
        applyRequest(event, req);
        CalendarEvent saved = eventRepository.save(event);
        
        notifyAssigneesOnUpdate(saved, oldAssignees, oldStartAt, oldTitle);
        
        return toResponse(saved);
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
        
        List<UUID> assignees = event.getAssigneeIds();
        
        event.softDelete();
        eventRepository.save(event);
        
        if (assignees != null && !assignees.isEmpty()) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("eventId", event.getId().toString());
            
            notificationService.send(
                    tenantId,
                    "CALENDAR_REMOVED",
                    "Evento cancelado: " + event.getTitle(),
                    "El evento ha sido cancelado o eliminado",
                    Notification.Severity.INFO,
                    metadata,
                    assignees);
        }
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

    private void notifyAssigneesOnCreate(CalendarEvent event) {
        List<UUID> assignees = event.getAssigneeIds();
        if (assignees == null || assignees.isEmpty()) return;

        UUID tenantId = event.getTenant().getId();
        String eventTitle = event.getTitle();
        String formattedDate = formatEventDateTime(event.getStartAt());
        String eventTypeLabel = event.getEventType().name();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("eventId", event.getId().toString());
        metadata.put("startAt", event.getStartAt().toString());
        metadata.put("endAt", event.getEndAt().toString());
        metadata.put("eventType", eventTypeLabel);
        if (event.getClientId() != null) {
            metadata.put("clientId", event.getClientId().toString());
            metadata.put("clientName", getClientName(event.getClientId()));
        }

        notificationService.send(
                tenantId,
                "CALENDAR_ASSIGNED",
                "Se te ha asignado un evento: " + eventTitle,
                formattedDate + " - " + eventTypeLabel,
                Notification.Severity.INFO,
                metadata,
                assignees);
    }

    private void notifyAssigneesOnUpdate(CalendarEvent event, List<UUID> oldAssignees, Instant oldStartAt, String oldTitle) {
        List<UUID> newAssignees = event.getAssigneeIds() != null ? event.getAssigneeIds() : Collections.emptyList();
        Set<UUID> oldSet = new HashSet<>(oldAssignees != null ? oldAssignees : Collections.emptyList());
        Set<UUID> newSet = new HashSet<>(newAssignees);

        Set<UUID> addedAssignees = new HashSet<>(newSet);
        addedAssignees.removeAll(oldSet);

        Set<UUID> removedAssignees = new HashSet<>(oldSet);
        removedAssignees.removeAll(newSet);

        UUID tenantId = event.getTenant().getId();
        String eventTitle = event.getTitle();
        String formattedDate = formatEventDateTime(event.getStartAt());
        String eventTypeLabel = event.getEventType().name();

        if (!addedAssignees.isEmpty()) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("eventId", event.getId().toString());
            metadata.put("startAt", event.getStartAt().toString());
            metadata.put("endAt", event.getEndAt().toString());
            metadata.put("eventType", eventTypeLabel);

            notificationService.send(
                    tenantId,
                    "CALENDAR_ASSIGNED",
                    "Se te ha asignado un evento: " + eventTitle,
                    formattedDate + " - " + eventTypeLabel,
                    Notification.Severity.INFO,
                    metadata,
                    new ArrayList<>(addedAssignees));
        }

        if (!removedAssignees.isEmpty()) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("eventId", event.getId().toString());

            notificationService.send(
                    tenantId,
                    "CALENDAR_REMOVED",
                    "Evento cancelado: " + eventTitle,
                    "El evento ha sido cancelado o eliminado",
                    Notification.Severity.INFO,
                    metadata,
                    new ArrayList<>(removedAssignees));
        }

        boolean titleChanged = !eventTitle.equals(oldTitle);
        boolean dateChanged = !event.getStartAt().equals(oldStartAt);

        if ((titleChanged || dateChanged) && !newSet.isEmpty()) {
            String changeType = titleChanged ? "título" : "fecha";

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("eventId", event.getId().toString());
            metadata.put("startAt", event.getStartAt().toString());
            metadata.put("eventType", eventTypeLabel);

            notificationService.send(
                    tenantId,
                    "CALENDAR_UPDATED",
                    "Evento actualizado: " + eventTitle,
                    "Se ha cambiado el " + changeType + " del evento",
                    Notification.Severity.INFO,
                    metadata,
                    new ArrayList<>(newSet));
        }
    }

    private String formatEventDateTime(Instant instant) {
        if (instant == null) return "";
        return instant.atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMAT);
    }

    private String getClientName(UUID clientId) {
        if (clientId == null) return null;
        return clientRepository.findById(clientId).map(c -> c.getName()).orElse(null);
    }
}
