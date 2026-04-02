package com.controltower.app.licenses.application;

import com.controltower.app.licenses.api.dto.ActivateLicenseRequest;
import com.controltower.app.licenses.api.dto.LicenseResponse;
import com.controltower.app.licenses.domain.*;
import com.controltower.app.shared.annotation.Audited;
import com.controltower.app.shared.exception.ControlTowerException;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LicenseService {

    private static final int GRACE_PERIOD_DAYS = 7;

    private final LicenseRepository      licenseRepository;
    private final PlanRepository         planRepository;
    private final LicenseEventRepository eventRepository;

    @Transactional(readOnly = true)
    public Page<LicenseResponse> listLicenses(Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        return licenseRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public LicenseResponse getLicense(UUID licenseId) {
        return toResponse(resolve(licenseId));
    }

    @Transactional(readOnly = true)
    public LicenseResponse getLicenseByClient(UUID clientId) {
        License license = licenseRepository.findByClientIdAndDeletedAtIsNull(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("License for client", clientId));
        return toResponse(license);
    }

    @Transactional
    @Audited(action = "LICENSE_ACTIVATED", resource = "License")
    public LicenseResponse activate(ActivateLicenseRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan", request.getPlanId()));

        // Only one license per client
        licenseRepository.findByClientIdAndDeletedAtIsNull(request.getClientId())
                .ifPresent(l -> {
                    throw new ControlTowerException(
                        "Client already has an active license", HttpStatus.CONFLICT);
                });

        License license = new License();
        license.setTenantId(tenantId);
        license.setClientId(request.getClientId());
        license.setPlan(plan);
        license.setCurrentPeriodEnd(
                Instant.now().plus(request.getTrialDays() != null ? request.getTrialDays() : 14,
                        ChronoUnit.DAYS));
        licenseRepository.save(license);
        recordEvent(license, "LICENSE_ACTIVATED", Map.of("planId", plan.getId().toString()));
        return toResponse(license);
    }

    @Transactional
    @Audited(action = "LICENSE_SUSPENDED", resource = "License")
    public LicenseResponse suspend(UUID licenseId) {
        License license = resolve(licenseId);
        if (license.getStatus() == License.LicenseStatus.SUSPENDED) {
            throw new ControlTowerException("License is already suspended", HttpStatus.BAD_REQUEST);
        }
        license.suspend();
        licenseRepository.save(license);
        recordEvent(license, "LICENSE_SUSPENDED", null);
        return toResponse(license);
    }

    @Transactional
    @Audited(action = "LICENSE_REACTIVATED", resource = "License")
    public LicenseResponse reactivate(UUID licenseId, int extensionDays) {
        License license = resolve(licenseId);
        Instant newEnd = Instant.now().plus(extensionDays, ChronoUnit.DAYS);
        license.reactivate(newEnd);
        licenseRepository.save(license);
        recordEvent(license, "LICENSE_REACTIVATED",
                Map.of("extensionDays", String.valueOf(extensionDays)));
        return toResponse(license);
    }

    @Transactional
    @Audited(action = "LICENSE_CANCELLED", resource = "License")
    public LicenseResponse cancel(UUID licenseId) {
        License license = resolve(licenseId);
        if (license.getStatus() == License.LicenseStatus.CANCELLED) {
            throw new ControlTowerException("License is already cancelled", HttpStatus.BAD_REQUEST);
        }
        license.cancel();
        licenseRepository.save(license);
        recordEvent(license, "LICENSE_CANCELLED", null);
        return toResponse(license);
    }

    @Transactional(readOnly = true)
    public List<String> getEnabledFeatures(UUID licenseId) {
        License license = resolve(licenseId);
        return license.getPlan().getFeatures().stream()
                .filter(PlanFeature::isEnabled)
                .map(PlanFeature::getFeatureCode)
                .toList();
    }

    /** Called by LicenseScheduler — no TenantContext needed. */
    @Transactional
    public void processExpiring(Instant threshold) {
        licenseRepository.findExpiring(threshold).forEach(license -> {
            license.enterGracePeriod(Instant.now().plus(GRACE_PERIOD_DAYS, ChronoUnit.DAYS));
            licenseRepository.save(license);
            recordEvent(license, "LICENSE_GRACE_PERIOD_STARTED", null);
        });
    }

    /** Called by LicenseScheduler — no TenantContext needed. */
    @Transactional
    public void processGraceExpired() {
        licenseRepository.findGraceExpired(Instant.now()).forEach(license -> {
            license.suspend();
            licenseRepository.save(license);
            recordEvent(license, "LICENSE_SUSPENDED_GRACE_EXPIRED", null);
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private License resolve(UUID licenseId) {
        UUID tenantId = TenantContext.getTenantId();
        License license = licenseRepository.findById(licenseId)
                .orElseThrow(() -> new ResourceNotFoundException("License", licenseId));
        if (tenantId != null && !tenantId.equals(license.getTenantId())) {
            throw new ResourceNotFoundException("License", licenseId);
        }
        return license;
    }

    private void recordEvent(License license, String eventType, Map<String, Object> payload) {
        LicenseEvent event = new LicenseEvent();
        event.setLicense(license);
        event.setEventType(eventType);
        event.setPayload(payload);
        event.setProcessedAt(Instant.now());
        eventRepository.save(event);
    }

    private LicenseResponse toResponse(License l) {
        return LicenseResponse.builder()
                .id(l.getId())
                .tenantId(l.getTenantId())
                .clientId(l.getClientId())
                .planId(l.getPlan().getId())
                .planName(l.getPlan().getName())
                .status(l.getStatus().name())
                .currentPeriodStart(l.getCurrentPeriodStart())
                .currentPeriodEnd(l.getCurrentPeriodEnd())
                .gracePeriodEnd(l.getGracePeriodEnd())
                .stripeSubscriptionId(l.getStripeSubscriptionId())
                .build();
    }
}
