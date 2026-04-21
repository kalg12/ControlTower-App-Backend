package com.controltower.app.reminders.api;

import com.controltower.app.reminders.application.ClientReminderService;
import com.controltower.app.reminders.domain.ClientReminder;
import com.controltower.app.reminders.domain.ClientReminderHistory;
import com.controltower.app.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Client Reminders", description = "Recurring reminders for client follow-ups")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/reminders")
@RequiredArgsConstructor
public class ClientReminderController {

    private final ClientReminderService reminderService;

    @Operation(summary = "List reminders", description = "List all reminders with optional filters")
    @GetMapping
    @PreAuthorize("hasAuthority('client:read')")
    public ResponseEntity<ApiResponse<Page<ClientReminder>>> list(
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) ClientReminder.ReminderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                reminderService.listReminders(clientId, status, PageRequest.of(page, size))));
    }

    @Operation(summary = "Get reminders for client", description = "Get active reminders for a specific client")
    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAuthority('client:read')")
    public ResponseEntity<ApiResponse<List<ClientReminder>>> getByClient(@PathVariable UUID clientId) {
        return ResponseEntity.ok(ApiResponse.ok(
                reminderService.getActiveRemindersForClient(clientId)));
    }

    @Operation(summary = "Create reminder", description = "Create a new recurring reminder")
    @PostMapping
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<ClientReminder>> create(
            @RequestBody ClientReminder reminder,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(reminderService.createReminder(reminder, userId)));
    }

    @Operation(summary = "Update reminder", description = "Update an existing reminder")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<ClientReminder>> update(
            @PathVariable UUID id,
            @RequestBody ClientReminder reminder) {
        return ResponseEntity.ok(ApiResponse.ok(reminderService.updateReminder(id, reminder)));
    }

    @Operation(summary = "Complete reminder", description = "Mark a reminder as completed and schedule next occurrence")
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<Void>> complete(
            @PathVariable UUID id,
            @RequestParam(required = false) String notes,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        reminderService.completeReminder(id, userId, notes);
        return ResponseEntity.ok(ApiResponse.ok("Reminder completed"));
    }

    @Operation(summary = "Snooze reminder", description = "Postpone a reminder by specified days")
    @PostMapping("/{id}/snooze")
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<Void>> snooze(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "1") int days,
            @RequestParam(required = false) String notes,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        reminderService.snoozeReminder(id, userId, days, notes);
        return ResponseEntity.ok(ApiResponse.ok("Reminder snoozed"));
    }

    @Operation(summary = "Pause reminder", description = "Pause an active reminder")
    @PostMapping("/{id}/pause")
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<Void>> pause(@PathVariable UUID id) {
        reminderService.pauseReminder(id);
        return ResponseEntity.ok(ApiResponse.ok("Reminder paused"));
    }

    @Operation(summary = "Resume reminder", description = "Resume a paused reminder")
    @PostMapping("/{id}/resume")
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<Void>> resume(@PathVariable UUID id) {
        reminderService.resumeReminder(id);
        return ResponseEntity.ok(ApiResponse.ok("Reminder resumed"));
    }

    @Operation(summary = "Delete reminder", description = "Delete a reminder")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        reminderService.deleteReminder(id);
        return ResponseEntity.ok(ApiResponse.ok("Reminder deleted"));
    }

    @Operation(summary = "Get reminder history", description = "Get completion history for a reminder")
    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('client:read')")
    public ResponseEntity<ApiResponse<List<ClientReminderHistory>>> getHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(reminderService.getHistory(id)));
    }
}