package com.controltower.app.monitoring.application;

import com.controltower.app.identity.domain.UserRepository;
import com.controltower.app.integrations.domain.IntegrationEndpoint;
import com.controltower.app.integrations.domain.IntegrationEndpointRepository;
import com.controltower.app.monitoring.api.dto.LogEntryDto;
import com.controltower.app.monitoring.api.dto.LogIngestRequest;
import com.controltower.app.monitoring.api.dto.RemoteLogResponse;
import com.controltower.app.monitoring.domain.RemoteLog;
import com.controltower.app.monitoring.domain.RemoteLogRepository;
import com.controltower.app.monitoring.domain.RemoteLogSpecification;
import com.controltower.app.notifications.application.NotificationService;
import com.controltower.app.notifications.domain.Notification;
import com.controltower.app.shared.infrastructure.AesEncryptor;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.shared.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RemoteLogService {

    private final RemoteLogRepository         remoteLogRepository;
    private final IntegrationEndpointRepository endpointRepository;
    private final UserRepository              userRepository;
    private final NotificationService         notificationService;
    private final AesEncryptor                aesEncryptor;

    @Transactional
    public void ingest(LogIngestRequest request, String apiKey) {
        IntegrationEndpoint endpoint = resolveAndValidate(request.getEndpointId(), apiKey);
        UUID tenantId = endpoint.getTenantId();

        List<RemoteLog> saved = request.getEntries().stream()
                .map(dto -> toEntity(dto, tenantId, endpoint.getId()))
                .map(remoteLogRepository::save)
                .toList();

        // Push real-time notification for each ERROR/CRITICAL entry
        List<UUID> recipients = userRepository
                .findByTenantIdAndPermission(tenantId, "integration:read")
                .stream().map(u -> u.getId()).toList();

        if (!recipients.isEmpty()) {
            for (RemoteLog log : saved) {
                if (log.getLevel() == RemoteLog.Level.ERROR || log.getLevel() == RemoteLog.Level.CRITICAL) {
                    String service = log.getServiceName() != null ? log.getServiceName() : "Application";
                    String title = "[" + (log.getBusinessName() != null ? log.getBusinessName() : "POS") + "] " + service;
                    String body = log.getMessage().length() > 200
                            ? log.getMessage().substring(0, 200) + "…"
                            : log.getMessage();
                    notificationService.send(
                            tenantId,
                            "REMOTE_LOG_ERROR",
                            title,
                            body,
                            log.getLevel() == RemoteLog.Level.CRITICAL
                                    ? Notification.Severity.CRITICAL
                                    : Notification.Severity.ERROR,
                            Map.of("logId", log.getId().toString(), "level", log.getLevel().name()),
                            recipients
                    );
                }
            }
        }
    }

    public PageResponse<RemoteLogResponse> list(UUID tenantId,
                                                String level,
                                                String service,
                                                String business,
                                                Instant from,
                                                Instant to,
                                                int page,
                                                int size) {
        RemoteLog.Level parsedLevel = null;
        if (level != null && !level.isBlank()) {
            try { parsedLevel = RemoteLog.Level.valueOf(level.toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }
        var pageable = PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("receivedAt").descending());
        var result = remoteLogRepository.findAll(
                RemoteLogSpecification.filter(tenantId, parsedLevel, service, business, from, to),
                pageable
        );
        return PageResponse.from(result.map(RemoteLogResponse::from));
    }

    private IntegrationEndpoint resolveAndValidate(UUID endpointId, String apiKey) {
        IntegrationEndpoint endpoint = endpointRepository.findById(endpointId)
                .orElseThrow(() -> new ResourceNotFoundException("IntegrationEndpoint", endpointId));
        if (!endpoint.isActive()) {
            throw new ResourceNotFoundException("IntegrationEndpoint", endpointId);
        }
        if (endpoint.getApiKey() != null) {
            String decrypted = aesEncryptor.decrypt(endpoint.getApiKey());
            if (!decrypted.equals(apiKey)) {
                throw new SecurityException("Invalid API key");
            }
        }
        return endpoint;
    }

    private RemoteLog toEntity(LogEntryDto dto, UUID tenantId, UUID endpointId) {
        RemoteLog log = new RemoteLog();
        log.setTenantId(tenantId);
        log.setEndpointId(endpointId);
        log.setMessage(dto.getMessage());
        log.setServiceName(dto.getServiceName());
        log.setStackTrace(dto.getStackTrace());
        log.setBusinessName(dto.getBusinessName());
        log.setSource(dto.getSource() != null ? dto.getSource() : "POS");
        log.setMetadata(dto.getMetadata());

        RemoteLog.Level lvl = RemoteLog.Level.WARN;
        if (dto.getLevel() != null) {
            try { lvl = RemoteLog.Level.valueOf(dto.getLevel().toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }
        log.setLevel(lvl);
        return log;
    }
}
