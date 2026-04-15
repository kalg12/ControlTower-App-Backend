package com.controltower.app.time.application;

import com.controltower.app.identity.domain.Tenant;
import com.controltower.app.identity.domain.TenantRepository;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.support.domain.Ticket.Priority;
import com.controltower.app.tenancy.domain.TenantConfig;
import com.controltower.app.tenancy.domain.TenantConfigRepository;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages configurable SLA time windows per tenant.
 * Values are stored as {@link TenantConfig} key-value pairs:
 * {@code sla.hours.LOW}, {@code sla.hours.MEDIUM}, {@code sla.hours.HIGH}, {@code sla.hours.CRITICAL}.
 * Defaults match the original hard-coded values (48 / 24 / 8 / 2 hours).
 */
@Service
@RequiredArgsConstructor
public class SlaConfigService {

    private static final Map<Priority, Integer> DEFAULTS = Map.of(
            Priority.LOW,      48,
            Priority.MEDIUM,   24,
            Priority.HIGH,      8,
            Priority.CRITICAL,  2
    );

    private final TenantConfigRepository configRepository;
    private final TenantRepository       tenantRepository;

    // ── Read ─────────────────────────────────────────────────────────

    /** Returns the SLA window in hours for the given priority in the current tenant. */
    @Transactional(readOnly = true)
    public int getWindowHours(Priority priority) {
        UUID tenantId = TenantContext.getTenantId();
        return configRepository
                .findByTenantIdAndKey(tenantId, keyFor(priority))
                .map(c -> parseInt(c.getValue(), DEFAULTS.get(priority)))
                .orElseGet(() -> DEFAULTS.get(priority));
    }

    /** Returns all four SLA windows for the current tenant. */
    @Transactional(readOnly = true)
    public Map<Priority, Integer> getAllWindows() {
        UUID tenantId = TenantContext.getTenantId();
        Map<Priority, Integer> result = new EnumMap<>(Priority.class);
        for (Priority p : Priority.values()) {
            result.put(p, configRepository
                    .findByTenantIdAndKey(tenantId, keyFor(p))
                    .map(c -> parseInt(c.getValue(), DEFAULTS.get(p)))
                    .orElseGet(() -> DEFAULTS.get(p)));
        }
        return result;
    }

    // ── Write ────────────────────────────────────────────────────────

    /**
     * Persists new SLA window hours for each priority provided.
     * Accepts a partial map (only the priorities that should change).
     */
    @Transactional
    public void updateWindows(Map<Priority, Integer> updates) {
        UUID tenantId = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + tenantId));

        for (Map.Entry<Priority, Integer> entry : updates.entrySet()) {
            String key   = keyFor(entry.getKey());
            String value = String.valueOf(Math.max(1, entry.getValue())); // min 1 hour
            configRepository.findByTenantIdAndKey(tenantId, key)
                    .ifPresentOrElse(
                            cfg -> cfg.setValue(value),
                            () -> {
                                TenantConfig cfg = new TenantConfig();
                                cfg.setTenant(tenant);
                                cfg.setKey(key);
                                cfg.setValue(value);
                                configRepository.save(cfg);
                            }
                    );
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private static String keyFor(Priority priority) {
        return "sla.hours." + priority.name();
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
