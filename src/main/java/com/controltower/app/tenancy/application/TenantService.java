package com.controltower.app.tenancy.application;

import com.controltower.app.identity.domain.Tenant;
import com.controltower.app.identity.domain.TenantRepository;
import com.controltower.app.shared.exception.ControlTowerException;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.tenancy.api.dto.TenantRequest;
import com.controltower.app.tenancy.api.dto.TenantResponse;
import com.controltower.app.tenancy.domain.TenantConfig;
import com.controltower.app.tenancy.domain.TenantConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantConfigRepository tenantConfigRepository;

    @Transactional(readOnly = true)
    public Page<TenantResponse> listTenants(Pageable pageable) {
        return tenantRepository.findAllByDeletedAtIsNull(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TenantResponse getTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
        return toResponse(tenant);
    }

    @Transactional
    public TenantResponse createTenant(TenantRequest request) {
        if (tenantRepository.existsBySlugAndDeletedAtIsNull(request.getSlug())) {
            throw new ControlTowerException("Slug already in use: " + request.getSlug(), HttpStatus.CONFLICT);
        }
        Tenant tenant = new Tenant();
        tenant.setName(request.getName());
        tenant.setSlug(request.getSlug());
        if (request.getCountry()  != null) tenant.setCountry(request.getCountry());
        if (request.getTimezone() != null) tenant.setTimezone(request.getTimezone());
        if (request.getCurrency() != null) tenant.setCurrency(request.getCurrency());
        return toResponse(tenantRepository.save(tenant));
    }

    @Transactional
    public TenantResponse updateTenant(UUID tenantId, TenantRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
        tenant.setName(request.getName());
        if (request.getCountry()  != null) tenant.setCountry(request.getCountry());
        if (request.getTimezone() != null) tenant.setTimezone(request.getTimezone());
        if (request.getCurrency() != null) tenant.setCurrency(request.getCurrency());
        return toResponse(tenantRepository.save(tenant));
    }

    @Transactional
    public void suspendTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
        tenant.setStatus(Tenant.TenantStatus.SUSPENDED);
        tenantRepository.save(tenant);
    }

    @Transactional
    public void reactivateTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        tenantRepository.save(tenant);
    }

    @Transactional
    public void deleteTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
        tenant.setStatus(Tenant.TenantStatus.CANCELLED);
        tenant.softDelete();
        tenantRepository.save(tenant);
    }

    // ── Tenant config ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, String> getConfig(UUID tenantId) {
        return tenantConfigRepository.findByTenantId(tenantId)
                .stream()
                .collect(Collectors.toMap(TenantConfig::getKey, TenantConfig::getValue));
    }

    @Transactional
    public void setConfig(UUID tenantId, String key, String value) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        TenantConfig config = tenantConfigRepository
                .findByTenantIdAndKey(tenantId, key)
                .orElseGet(() -> {
                    TenantConfig c = new TenantConfig();
                    c.setTenant(tenant);
                    c.setKey(key);
                    return c;
                });
        config.setValue(value);
        tenantConfigRepository.save(config);
    }

    // ── Mapping ───────────────────────────────────────────────────────

    private TenantResponse toResponse(Tenant tenant) {
        return TenantResponse.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .slug(tenant.getSlug())
                .status(tenant.getStatus().name())
                .country(tenant.getCountry())
                .timezone(tenant.getTimezone())
                .currency(tenant.getCurrency())
                .createdAt(tenant.getCreatedAt())
                .build();
    }
}
