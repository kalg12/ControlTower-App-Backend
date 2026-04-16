package com.controltower.app.time.api;

import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.time.api.dto.*;
import com.controltower.app.time.application.TimeEntryService;
import com.controltower.app.time.domain.TimeEntry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Time Tracking", description = "Work-log timers and manual time entries for tickets and kanban cards")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/time-entries")
@RequiredArgsConstructor
public class TimeEntryController {

    private final TimeEntryService timeEntryService;

    // ── Timer control ─────────────────────────────────────────────────

    @Operation(summary = "Start a timer for a ticket or card",
               description = "Creates a running time entry. Any previously active timer for this user is automatically stopped.")
    @PostMapping("/start")
    @PreAuthorize("hasAuthority('ticket:write') or hasAuthority('kanban:write')")
    public ResponseEntity<ApiResponse<TimeEntryResponse>> startTimer(
            @Valid @RequestBody StartTimerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Timer started", timeEntryService.startTimer(request)));
    }

    @Operation(summary = "Stop a running timer")
    @PatchMapping("/{id}/stop")
    @PreAuthorize("hasAuthority('ticket:write') or hasAuthority('kanban:write')")
    public ResponseEntity<ApiResponse<TimeEntryResponse>> stopTimer(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Timer stopped", timeEntryService.stopTimer(id)));
    }

    @Operation(summary = "Get the current user's active (running) timer, if any")
    @GetMapping("/active")
    @PreAuthorize("hasAuthority('ticket:read') or hasAuthority('kanban:read')")
    public ResponseEntity<ApiResponse<TimeEntryResponse>> getActiveTimer() {
        return timeEntryService.getActiveTimer()
                .map(e -> ResponseEntity.ok(ApiResponse.ok("Active timer", e)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.<TimeEntryResponse>ok("No active timer", null)));
    }

    // ── Manual log ────────────────────────────────────────────────────

    @Operation(summary = "Manually log time (no live timer needed)")
    @PostMapping("/log")
    @PreAuthorize("hasAuthority('ticket:write') or hasAuthority('kanban:write')")
    public ResponseEntity<ApiResponse<TimeEntryResponse>> logManual(
            @Valid @RequestBody LogTimeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Time logged", timeEntryService.logManual(request)));
    }

    // ── List & summary ────────────────────────────────────────────────

    @Operation(summary = "List all time entries for a ticket or card")
    @GetMapping
    @PreAuthorize("hasAuthority('ticket:read') or hasAuthority('kanban:read')")
    public ResponseEntity<ApiResponse<List<TimeEntryResponse>>> listEntries(
            @RequestParam TimeEntry.EntityType entityType,
            @RequestParam UUID entityId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Time entries", timeEntryService.listEntries(entityType, entityId)));
    }

    @Operation(summary = "Get time summary (estimated vs logged) for a ticket or card")
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('ticket:read') or hasAuthority('kanban:read')")
    public ResponseEntity<ApiResponse<TimeSummaryResponse>> getSummary(
            @RequestParam TimeEntry.EntityType entityType,
            @RequestParam UUID entityId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Time summary", timeEntryService.getSummary(entityType, entityId)));
    }

    // ── Delete ────────────────────────────────────────────────────────

    @Operation(summary = "Delete a time entry (soft delete)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ticket:write') or hasAuthority('kanban:write')")
    public ResponseEntity<ApiResponse<Void>> deleteEntry(@PathVariable UUID id) {
        timeEntryService.deleteEntry(id);
        return ResponseEntity.ok(ApiResponse.<Void>ok("Time entry deleted", null));
    }
}
