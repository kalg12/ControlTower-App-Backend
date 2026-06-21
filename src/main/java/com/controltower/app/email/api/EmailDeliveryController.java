package com.controltower.app.email.api;

import com.controltower.app.email.api.dto.DeliveryResponse;
import com.controltower.app.email.application.EmailOutboundService;
import com.controltower.app.email.domain.EmailDelivery;
import com.controltower.app.email.domain.EmailDeliveryRepository;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.shared.response.PageResponse;
import com.controltower.app.tenancy.domain.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Email — Deliveries", description = "Outbound email delivery logs and retry")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/email/deliveries")
@RequiredArgsConstructor
public class EmailDeliveryController {

    private final EmailDeliveryRepository deliveryRepo;
    private final EmailOutboundService outboundService;

    @GetMapping
    @PreAuthorize("hasAuthority('email:read')")
    @Operation(summary = "List outbound email delivery logs")
    public ResponseEntity<ApiResponse<PageResponse<DeliveryResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID tenantId = TenantContext.getTenantId();
        Page<DeliveryResponse> data = deliveryRepo.findByTenantId(
            tenantId,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).map(DeliveryResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(data)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('email:read')")
    @Operation(summary = "Get a delivery log entry")
    public ResponseEntity<ApiResponse<DeliveryResponse>> get(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        EmailDelivery delivery = deliveryRepo.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Delivery not found"));
        return ResponseEntity.ok(ApiResponse.ok(DeliveryResponse.from(delivery)));
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAuthority('email:write')")
    @Operation(summary = "Manually retry a failed delivery")
    public ResponseEntity<ApiResponse<DeliveryResponse>> retry(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        EmailDelivery delivery = deliveryRepo.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Delivery not found"));
        delivery.setAttempts(0);
        delivery.setStatus(EmailDelivery.DeliveryStatus.QUEUED);
        delivery.setNextRetryAt(null);
        outboundService.attempt(delivery);
        return ResponseEntity.ok(ApiResponse.ok("Retry triggered", DeliveryResponse.from(delivery)));
    }
}
