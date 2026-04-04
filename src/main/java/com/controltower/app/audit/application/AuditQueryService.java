package com.controltower.app.audit.application;

import com.controltower.app.audit.api.dto.AuditLogResponse;
import com.controltower.app.audit.domain.AuditAction;
import com.controltower.app.audit.domain.AuditLog;
import com.controltower.app.audit.domain.AuditLogRepository;
import com.controltower.app.shared.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> query(
            UUID tenantId,
            UUID userId,
            AuditAction action,
            Instant from,
            Instant to,
            Pageable pageable) {

        Page<AuditLog> page = auditLogRepository.findAll(
                AuditLogSpecification.filter(tenantId, userId, action, from, to), pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .tenantId(log.getTenantId())
                .userId(log.getUserId())
                .action(log.getAction().name())
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .result(log.getResult().name())
                .ipAddress(log.getIpAddress())
                .correlationId(log.getCorrelationId())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
