package com.controltower.app.audit.api;

import com.controltower.app.audit.api.dto.AuditLogResponse;
import com.controltower.app.audit.application.AuditQueryService;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.shared.response.PageResponse;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/crm/history")
@RequiredArgsConstructor
public class CrmHistoryController {

    private final AuditQueryService auditQueryService;

    @GetMapping("/client/{clientId}")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getClientHistory(
            @PathVariable UUID clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        UUID tenantId = TenantContext.getTenantId();
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        PageResponse<AuditLogResponse> result = auditQueryService.getHistoryByResource("CLIENT", clientId, pageable);
        
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getBranchHistory(
            @PathVariable UUID branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        UUID tenantId = TenantContext.getTenantId();
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        PageResponse<AuditLogResponse> result = auditQueryService.getHistoryByResource("BRANCH", branchId, pageable);
        
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/opportunity/{opportunityId}")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getOpportunityHistory(
            @PathVariable UUID opportunityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        UUID tenantId = TenantContext.getTenantId();
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        PageResponse<AuditLogResponse> result = auditQueryService.getHistoryByResource("OPPORTUNITY", opportunityId, pageable);
        
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/contact/{contactId}")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getContactHistory(
            @PathVariable UUID contactId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        UUID tenantId = TenantContext.getTenantId();
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        PageResponse<AuditLogResponse> result = auditQueryService.getHistoryByResource("CONTACT", contactId, pageable);
        
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
