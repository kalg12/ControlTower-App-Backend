package com.controltower.app.calendar.api;

import com.controltower.app.calendar.api.dto.CalendarEventRequest;
import com.controltower.app.calendar.api.dto.CalendarEventResponse;
import com.controltower.app.calendar.application.CalendarService;
import com.controltower.app.calendar.domain.CalendarEvent;
import com.controltower.app.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Tag(name = "Calendar", description = "CRM calendar — schedule and track client visits, calls, demos")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @Operation(summary = "List events", description = "Returns events in a date range. Optionally filter by clientId or assigneeId. Requires 'client:read'.")
    @GetMapping
    @PreAuthorize("hasAuthority('client:read')")
    public ResponseEntity<ApiResponse<List<CalendarEventResponse>>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) UUID personId,
            @RequestParam(required = false) UUID assigneeId) {
        Instant rangeFrom = from != null ? from : Instant.now().minusSeconds(86400L * 30);
        Instant rangeTo   = to   != null ? to   : Instant.now().plusSeconds(86400L * 365);
        return ResponseEntity.ok(ApiResponse.ok(calendarService.listEvents(rangeFrom, rangeTo, clientId, personId, assigneeId)));
    }

    @Operation(summary = "Create event", description = "Creates a new calendar event. Requires 'client:write'.")
    @PostMapping
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<CalendarEventResponse>> create(
            @Valid @RequestBody CalendarEventRequest request,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(calendarService.createEvent(request, userId)));
    }

    @Operation(summary = "Update event", description = "Updates all fields of a calendar event. Requires 'client:write'.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<CalendarEventResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CalendarEventRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(calendarService.updateEvent(id, request)));
    }

    @Operation(summary = "Patch event status", description = "Updates only the status (SCHEDULED/COMPLETED/CANCELLED/NO_SHOW). Requires 'client:write'.")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<CalendarEventResponse>> patchStatus(
            @PathVariable UUID id,
            @RequestParam CalendarEvent.EventStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(calendarService.patchStatus(id, status)));
    }

    @Operation(summary = "Delete event", description = "Soft-deletes a calendar event. Requires 'client:write'.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        calendarService.deleteEvent(id);
        return ResponseEntity.ok(ApiResponse.ok("Event deleted"));
    }
}
