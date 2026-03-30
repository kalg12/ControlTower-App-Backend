package com.controltower.app.tenancy.domain;

import java.util.UUID;

/**
 * ThreadLocal holder for the currently active tenant.
 *
 * The tenant is set per-request by TenantInterceptor (resolved from the
 * authenticated user's JWT claim) and cleared after the request completes.
 *
 * Usage:
 *   UUID tenantId = TenantContext.getTenantId();
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static boolean hasTenant() {
        return CURRENT_TENANT.get() != null;
    }

    /** Must be called at the end of every request to prevent memory leaks. */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
