package com.controltower.app.tenancy.api;

import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.tenancy.api.dto.OnboardingRequest;
import com.controltower.app.tenancy.api.dto.OnboardingResponse;
import com.controltower.app.tenancy.application.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public endpoint to onboard a new tenant (creates tenant, admin user, default role, and trial license).
 * No auth required so the first tenant can be created.
 */
@Tag(name = "Onboarding", description = "Tenant self-service onboarding")
@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @Operation(summary = "Onboard new tenant", description = "Self-service endpoint that creates a new tenant, an admin user, a default role, and a trial license in a single transaction. No authentication required.")
    @PostMapping("/onboard")
    public ResponseEntity<ApiResponse<OnboardingResponse>> onboard(
            @Valid @RequestBody OnboardingRequest request) {
        OnboardingResponse response = onboardingService.onboard(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tenant onboarded successfully", response));
    }
}
