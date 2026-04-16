package com.controltower.app.time.api;

import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.support.domain.Ticket;
import com.controltower.app.time.api.dto.SlaConfigResponse;
import com.controltower.app.time.api.dto.UpdateSlaConfigRequest;
import com.controltower.app.time.application.SlaConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.EnumMap;
import java.util.Map;

@Tag(name = "SLA Configuration", description = "Manage configurable SLA time windows per tenant")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/sla-config")
@RequiredArgsConstructor
public class SlaConfigController {

    private final SlaConfigService slaConfigService;

    @Operation(summary = "Get current SLA windows (hours) for all priorities")
    @GetMapping
    @PreAuthorize("hasAuthority('settings:read') or hasAuthority('tickets:read')")
    public ResponseEntity<ApiResponse<SlaConfigResponse>> getConfig() {
        Map<Ticket.Priority, Integer> windows = slaConfigService.getAllWindows();
        SlaConfigResponse response = SlaConfigResponse.builder()
                .low(windows.get(Ticket.Priority.LOW))
                .medium(windows.get(Ticket.Priority.MEDIUM))
                .high(windows.get(Ticket.Priority.HIGH))
                .critical(windows.get(Ticket.Priority.CRITICAL))
                .build();
        return ResponseEntity.ok(ApiResponse.ok("SLA configuration", response));
    }

    @Operation(summary = "Update SLA windows (hours) for all or specific priorities",
               description = "Only provided fields are updated; omitted priorities keep their current value.")
    @PutMapping
    @PreAuthorize("hasAuthority('settings:write')")
    public ResponseEntity<ApiResponse<SlaConfigResponse>> updateConfig(
            @Valid @RequestBody UpdateSlaConfigRequest request) {
        Map<Ticket.Priority, Integer> updates = new EnumMap<>(Ticket.Priority.class);
        if (request.getLow()      != null) updates.put(Ticket.Priority.LOW,      request.getLow());
        if (request.getMedium()   != null) updates.put(Ticket.Priority.MEDIUM,   request.getMedium());
        if (request.getHigh()     != null) updates.put(Ticket.Priority.HIGH,     request.getHigh());
        if (request.getCritical() != null) updates.put(Ticket.Priority.CRITICAL, request.getCritical());

        slaConfigService.updateWindows(updates);
        return getConfig();
    }
}
