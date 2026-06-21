package com.controltower.app.email.api;

import com.controltower.app.email.api.dto.RoutingRuleRequest;
import com.controltower.app.email.api.dto.RoutingRuleResponse;
import com.controltower.app.email.domain.EmailRoutingRule;
import com.controltower.app.email.domain.EmailRoutingRuleRepository;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.tenancy.domain.TenantContext;
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
import java.util.Map;
import java.util.UUID;

@Tag(name = "Email — Routing Rules", description = "Condition-based routing rules for inbound emails")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/email/rules")
@RequiredArgsConstructor
public class EmailRoutingRuleController {

    private final EmailRoutingRuleRepository ruleRepo;

    @GetMapping
    @PreAuthorize("hasAuthority('email:read')")
    @Operation(summary = "List all routing rules for the tenant")
    public ResponseEntity<ApiResponse<List<RoutingRuleResponse>>> list() {
        UUID tenantId = TenantContext.getTenantId();
        List<RoutingRuleResponse> data = ruleRepo.findByTenantId(tenantId)
            .stream()
            .sorted(java.util.Comparator.comparingInt(EmailRoutingRule::getPriority))
            .map(RoutingRuleResponse::from)
            .toList();
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('email:write')")
    @Operation(summary = "Create a new routing rule")
    public ResponseEntity<ApiResponse<RoutingRuleResponse>> create(@Valid @RequestBody RoutingRuleRequest req) {
        UUID tenantId = TenantContext.getTenantId();
        EmailRoutingRule rule = mapToEntity(req, new EmailRoutingRule());
        rule.setTenantId(tenantId);
        ruleRepo.save(rule);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Rule created", RoutingRuleResponse.from(rule)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('email:write')")
    @Operation(summary = "Update a routing rule")
    public ResponseEntity<ApiResponse<RoutingRuleResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody RoutingRuleRequest req) {
        UUID tenantId = TenantContext.getTenantId();
        EmailRoutingRule rule = ruleRepo.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Rule not found"));
        mapToEntity(req, rule);
        ruleRepo.save(rule);
        return ResponseEntity.ok(ApiResponse.ok("Rule updated", RoutingRuleResponse.from(rule)));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('email:write')")
    @Operation(summary = "Enable or disable a routing rule")
    public ResponseEntity<ApiResponse<RoutingRuleResponse>> toggle(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        EmailRoutingRule rule = ruleRepo.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Rule not found"));
        rule.setActive(!rule.isActive());
        ruleRepo.save(rule);
        return ResponseEntity.ok(ApiResponse.ok("Rule toggled", RoutingRuleResponse.from(rule)));
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasAuthority('email:write')")
    @Operation(summary = "Bulk update rule priorities — body: [{id, priority}]")
    public ResponseEntity<ApiResponse<Void>> reorder(@RequestBody List<Map<String, Object>> updates) {
        UUID tenantId = TenantContext.getTenantId();
        for (Map<String, Object> update : updates) {
            UUID id = UUID.fromString(update.get("id").toString());
            int priority = Integer.parseInt(update.get("priority").toString());
            ruleRepo.findByIdAndTenantId(id, tenantId).ifPresent(rule -> {
                rule.setPriority(priority);
                ruleRepo.save(rule);
            });
        }
        return ResponseEntity.ok(ApiResponse.ok("Priorities updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('email:manage')")
    @Operation(summary = "Delete a routing rule")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        EmailRoutingRule rule = ruleRepo.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Rule not found"));
        ruleRepo.delete(rule);
        return ResponseEntity.ok(ApiResponse.ok("Rule deleted"));
    }

    private EmailRoutingRule mapToEntity(RoutingRuleRequest req, EmailRoutingRule rule) {
        rule.setName(req.name());
        rule.setAliasId(req.aliasId());
        rule.setPriority(req.priority() != null ? req.priority() : 100);
        rule.setConditions(req.conditions());
        rule.setActions(req.actions());
        rule.setMatchMode(req.matchMode() != null ? req.matchMode().toUpperCase() : "ALL");
        rule.setSchedule(req.schedule());
        return rule;
    }
}
