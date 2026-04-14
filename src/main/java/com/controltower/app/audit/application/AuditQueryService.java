package com.controltower.app.audit.application;

import com.controltower.app.audit.api.dto.AuditLogResponse;
import com.controltower.app.audit.domain.AuditAction;
import com.controltower.app.audit.domain.AuditLog;
import com.controltower.app.audit.domain.AuditLogRepository;
import com.controltower.app.audit.domain.AuditLogSpecification;
import com.controltower.app.identity.domain.User;
import com.controltower.app.identity.domain.UserRepository;
import com.controltower.app.shared.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> query(
            UUID tenantId,
            UUID userId,
            AuditAction action,
            String resourceType,
            Instant from,
            Instant to,
            Pageable pageable) {

        Page<AuditLog> page = auditLogRepository.findAll(
                AuditLogSpecification.filter(tenantId, userId, action, resourceType, from, to), pageable);

        // Batch-resolve all user IDs to avoid N+1 queries
        Map<UUID, User> userCache = new HashMap<>();
        for (AuditLog log : page.getContent()) {
            if (log.getUserId() != null && !userCache.containsKey(log.getUserId())) {
                userRepository.findById(log.getUserId()).ifPresent(u -> userCache.put(log.getUserId(), u));
            }
        }

        return PageResponse.from(page.map(log -> toResponse(log, userCache)));
    }

    private AuditLogResponse toResponse(AuditLog log, Map<UUID, User> userCache) {
        User user = log.getUserId() != null ? userCache.get(log.getUserId()) : null;

        return AuditLogResponse.builder()
                .id(log.getId())
                .tenantId(log.getTenantId())
                .userId(log.getUserId())
                .userName(user != null ? user.getFullName() : null)
                .userEmail(user != null ? user.getEmail() : null)
                .action(log.getAction().name())
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .result(log.getResult().name())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .correlationId(log.getCorrelationId())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
