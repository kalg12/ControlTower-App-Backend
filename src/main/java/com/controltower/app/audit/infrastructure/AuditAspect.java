package com.controltower.app.audit.infrastructure;

import com.controltower.app.audit.application.AuditService;
import com.controltower.app.audit.domain.AuditAction;
import com.controltower.app.audit.domain.AuditLog;
import com.controltower.app.shared.annotation.Audited;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * AOP aspect that intercepts methods annotated with @Audited and
 * automatically creates an AuditLog entry on completion (or failure).
 *
 * Usage on a service method:
 *   {@literal @}Audited(action = "LICENSE_SUSPENDED", resource = "License")
 *   public void suspendLicense(UUID licenseId) { ... }
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId   = resolveUserId();
        AuditAction action = resolveAction(audited.action());

        try {
            Object result = joinPoint.proceed();

            auditService.log(AuditLog.builder(action)
                    .tenantId(tenantId)
                    .userId(userId)
                    .resource(audited.resource(), null)
                    .result(AuditLog.AuditResult.SUCCESS)
                    .build());

            return result;

        } catch (Throwable ex) {
            auditService.log(AuditLog.builder(action)
                    .tenantId(tenantId)
                    .userId(userId)
                    .resource(audited.resource(), null)
                    .result(AuditLog.AuditResult.FAILURE)
                    .newValue("{\"error\":\"" + ex.getMessage().replace("\"", "'") + "\"}")
                    .build());
            throw ex;
        }
    }

    private UUID resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            try {
                return UUID.fromString(auth.getName());
            } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }

    private AuditAction resolveAction(String actionName) {
        if (actionName == null || actionName.isBlank()) {
            return AuditAction.UPDATE;
        }
        try {
            return AuditAction.valueOf(actionName.toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown audit action '{}', defaulting to UPDATE", actionName);
            return AuditAction.UPDATE;
        }
    }
}
