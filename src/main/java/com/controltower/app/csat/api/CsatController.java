package com.controltower.app.csat.api;

import com.controltower.app.csat.application.CsatService;
import com.controltower.app.csat.domain.CsatSurvey;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Tag(name = "CSAT", description = "Customer Satisfaction surveys linked to tickets")
@RestController
@RequiredArgsConstructor
public class CsatController {

    private final CsatService csatService;

    // ── Authenticated endpoints ──────────────────────────────────────────────

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/v1/tickets/{ticketId}/csat")
    @PreAuthorize("hasAuthority('csat:write')")
    @Operation(summary = "Create or retrieve CSAT survey for a ticket",
               description = "Idempotent — returns existing survey if already sent for this ticket.")
    public ResponseEntity<ApiResponse<CsatSurvey>> createOrGet(@PathVariable UUID ticketId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Survey ready", csatService.createOrGet(ticketId)));
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/api/v1/csat")
    @PreAuthorize("hasAuthority('csat:read')")
    @Operation(summary = "List all CSAT surveys for the current tenant (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<CsatSurvey>>> list(
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "20")  int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(csatService.listByTenant(pageable))));
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/api/v1/tickets/{ticketId}/csat")
    @PreAuthorize("hasAuthority('csat:read')")
    @Operation(summary = "List CSAT surveys for a specific ticket")
    public ResponseEntity<ApiResponse<java.util.List<CsatSurvey>>> listByTicket(@PathVariable UUID ticketId) {
        return ResponseEntity.ok(ApiResponse.ok(csatService.listByTicket(ticketId)));
    }

    // ── Public endpoints (no auth — use token) ───────────────────────────────

    @GetMapping("/api/v1/survey/{token}")
    @Operation(summary = "Get survey details by token (public, no auth required)")
    public ResponseEntity<ApiResponse<CsatSurvey>> getByToken(@PathVariable UUID token) {
        return ResponseEntity.ok(ApiResponse.ok(csatService.getByToken(token)));
    }

    @PostMapping("/api/v1/survey/{token}")
    @Operation(summary = "Submit customer rating (public, no auth required)")
    public ResponseEntity<ApiResponse<CsatSurvey>> submit(
            @PathVariable UUID token,
            @RequestBody Map<String, Object> body) {
        short rating = ((Number) body.get("rating")).shortValue();
        String comment = body.containsKey("comment") ? (String) body.get("comment") : null;
        return ResponseEntity.ok(ApiResponse.ok("Thank you for your feedback!", csatService.submitRating(token, rating, comment)));
    }
}
