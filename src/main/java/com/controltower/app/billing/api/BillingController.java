package com.controltower.app.billing.api;

import com.controltower.app.billing.domain.BillingEvent;
import com.controltower.app.billing.domain.BillingEventRepository;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.shared.response.PageResponse;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Tag(name = "Billing", description = "Billing events and subscription history")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingEventRepository billingEventRepository;

    @Operation(summary = "List billing events", description = "Returns a paginated history of billing events (payments, refunds, subscription changes) for the current tenant. Requires the 'billing:read' permission.")
    @GetMapping("/events")
    @PreAuthorize("hasAuthority('billing:read')")
    public ResponseEntity<ApiResponse<PageResponse<BillingEvent>>> listEvents(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(
                ApiResponse.ok(PageResponse.from(
                        billingEventRepository.findByTenantIdOrderByCreatedAtDesc(
                                TenantContext.getTenantId(), pageable))));
    }
}
