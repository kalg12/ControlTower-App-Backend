package com.controltower.app.integrations.application;

import com.controltower.app.integrations.api.dto.IntegrationEndpointRequest;
import com.controltower.app.integrations.domain.*;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.shared.infrastructure.AesEncryptor;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IntegrationService {

    private final IntegrationEndpointRepository endpointRepository;
    private final IntegrationEventRepository    eventRepository;
    private final ApplicationEventPublisher     publisher;
    private final AesEncryptor                  aesEncryptor;

    @Transactional(readOnly = true)
    public Page<IntegrationEndpoint> listEndpoints(Pageable pageable) {
        return endpointRepository.findByTenantIdAndDeletedAtIsNull(
                TenantContext.getTenantId(), pageable);
    }

    @Transactional
    public IntegrationEndpoint register(IntegrationEndpointRequest request) {
        IntegrationEndpoint endpoint = new IntegrationEndpoint();
        endpoint.setTenantId(TenantContext.getTenantId());
        endpoint.setClientBranchId(request.getClientBranchId());
        endpoint.setType(request.getType());
        endpoint.setPullUrl(request.getPullUrl());
        endpoint.setApiKey(aesEncryptor.encrypt(request.getApiKey()));
        endpoint.setHeartbeatIntervalSeconds(request.getHeartbeatIntervalSeconds());
        endpoint.setContractVersion(request.getContractVersion());
        endpoint.setMetadata(request.getMetadata());
        return endpointRepository.save(endpoint);
    }

    @Transactional
    public IntegrationEndpoint update(UUID endpointId, IntegrationEndpointRequest request) {
        IntegrationEndpoint endpoint = resolve(endpointId);
        endpoint.setPullUrl(request.getPullUrl());
        endpoint.setApiKey(aesEncryptor.encrypt(request.getApiKey()));
        endpoint.setHeartbeatIntervalSeconds(request.getHeartbeatIntervalSeconds());
        endpoint.setContractVersion(request.getContractVersion());
        endpoint.setMetadata(request.getMetadata());
        return endpointRepository.save(endpoint);
    }

    /** Returns the decrypted API key for comparison. */
    public String getDecryptedApiKey(IntegrationEndpoint endpoint) {
        return aesEncryptor.decrypt(endpoint.getApiKey());
    }

    @Transactional
    public IntegrationEndpoint activate(UUID endpointId) {
        IntegrationEndpoint endpoint = resolve(endpointId);
        endpoint.setActive(true);
        return endpointRepository.save(endpoint);
    }

    @Transactional
    public void deactivate(UUID endpointId) {
        IntegrationEndpoint endpoint = resolve(endpointId);
        endpoint.setActive(false);
        endpointRepository.save(endpoint);
    }

    /**
     * Ingests a push event from an external system.
     * Public endpoint — authenticated via API key in header (validated here).
     */
    @Transactional
    public IntegrationEvent ingestEvent(UUID endpointId, String apiKey,
                                         String eventType, Map<String, Object> payload) {
        IntegrationEndpoint endpoint = endpointRepository.findById(endpointId)
                .orElseThrow(() -> new ResourceNotFoundException("IntegrationEndpoint", endpointId));

        if (!endpoint.isActive()) {
            throw new ResourceNotFoundException("IntegrationEndpoint", endpointId);
        }
        if (endpoint.getApiKey() != null) {
            String decryptedKey = aesEncryptor.decrypt(endpoint.getApiKey());
            if (!decryptedKey.equals(apiKey)) {
                throw new SecurityException("Invalid API key");
            }
        }

        IntegrationEvent event = new IntegrationEvent();
        event.setEndpoint(endpoint);
        event.setEventType(eventType);
        event.setPayload(payload);
        event.setProcessedAt(Instant.now());
        eventRepository.save(event);

        return event;
    }

    private IntegrationEndpoint resolve(UUID endpointId) {
        return endpointRepository.findByIdAndTenantIdAndDeletedAtIsNull(
                        endpointId, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("IntegrationEndpoint", endpointId));
    }
}
