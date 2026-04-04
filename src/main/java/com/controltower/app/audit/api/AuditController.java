package com.controltower.app.audit.api;

import com.controltower.app.audit.api.dto.AuditLogResponse;
import com.controltower.app.audit.application.AuditQueryService;
import com.controltower.app.audit.domain.AuditAction;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.shared.response.PageResponse;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

/**
 * Read-only audit log endpoint.
 * Supports filtering by userId, action, and date range.
 */
@Tag(name = "Audit", description = "Immutable audit log (read-only)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping({"/api/v1/audit", "/api/v1/audit-logs"})
@RequiredArgsConstructor
public class AuditController {

    private final AuditQueryService auditQueryService;

    @Operation(summary = "Query audit logs", description = "Returns a paginated, filterable view of the immutable audit log for the current tenant. Supports filtering by user, action type, and date range. Requires the 'audit:read' permission.")
    @GetMapping
    @PreAuthorize("hasAuthority('audit:read')")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> queryAuditLogs(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {

        UUID tenantId = TenantContext.getTenantId();
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponse<AuditLogResponse> result =
                auditQueryService.query(tenantId, userId, action, from, to, pageable);

        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
