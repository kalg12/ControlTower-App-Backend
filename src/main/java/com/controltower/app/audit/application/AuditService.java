package com.controltower.app.audit.application;

import com.controltower.app.audit.domain.AuditAction;
import com.controltower.app.audit.domain.AuditLog;
import com.controltower.app.audit.domain.AuditLog.AuditResult;
import com.controltower.app.audit.domain.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Writes audit records in an isolated transaction so that audit failures
 * never block or roll back the main business transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Log an audit event in a separate transaction so errors here do not
     * affect the calling transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditLog entry) {
        try {
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            // Never let audit failure propagate — just log the error
            log.error("Failed to persist audit log [action={}]: {}", entry.getAction(), ex.getMessage());
        }
    }

    /** Convenience method for the most common case. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditAction action, UUID tenantId, UUID userId, String resourceType, String resourceId) {
        log(AuditLog.builder(action)
                .tenantId(tenantId)
                .userId(userId)
                .resource(resourceType, resourceId)
                .result(AuditResult.SUCCESS)
                .build());
    }

    /** Log a failed action. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(AuditAction action, UUID tenantId, UUID userId, String details) {
        log(AuditLog.builder(action)
                .tenantId(tenantId)
                .userId(userId)
                .result(AuditResult.FAILURE)
                .newValue("{\"error\":\"" + details.replace("\"", "'") + "\"}")
                .build());
    }
}
