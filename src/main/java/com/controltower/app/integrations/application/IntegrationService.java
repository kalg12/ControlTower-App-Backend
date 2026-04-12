package com.controltower.app.integrations.application;

import com.controltower.app.integrations.api.dto.IntegrationEndpointRequest;
import com.controltower.app.integrations.api.dto.PosTicketCommentDto;
import com.controltower.app.integrations.api.dto.PosTicketStatusResponse;
import com.controltower.app.integrations.domain.*;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.shared.infrastructure.AesEncryptor;
import com.controltower.app.support.application.TicketService;
import com.controltower.app.support.domain.PosTicketChatEvent;
import com.controltower.app.support.domain.Ticket;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationService {

    private static final String POS_TICKET_EVENT = "POS_SUPPORT_TICKET";

    private final IntegrationEndpointRepository endpointRepository;
    private final IntegrationEventRepository    eventRepository;
    private final ApplicationEventPublisher     publisher;
    private final AesEncryptor                  aesEncryptor;
    private final TicketService                 ticketService;

    @Transactional(readOnly = true)
    public Page<IntegrationEndpoint> listEndpoints(Pageable pageable, String type) {
        UUID tenantId = TenantContext.getTenantId();
        if (type != null && !type.isBlank()) {
            IntegrationEndpoint.EndpointType endpointType =
                    IntegrationEndpoint.EndpointType.valueOf(type.toUpperCase());
            return endpointRepository.findByTenantIdAndTypeAndDeletedAtIsNull(
                    tenantId, endpointType, pageable);
        }
        return endpointRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable);
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

        if (POS_TICKET_EVENT.equals(eventType)) {
            processPosTicket(endpoint, payload);
        } else if ("POS_CHAT_MESSAGE".equals(eventType)) {
            processPosChat(endpoint, payload);
        }

        return event;
    }

    /** Returns the status summary of the CT ticket linked to a POS ticket ID. */
    public PosTicketStatusResponse getPosTicketStatus(UUID endpointId, String apiKey, String posTicketId) {
        IntegrationEndpoint endpoint = resolveAndValidateApiKey(endpointId, apiKey);
        return ticketService.getStatusForPosTicket(posTicketId, endpoint.getTenantId());
    }

    /** Returns public comments on the CT ticket linked to a POS ticket ID, optionally since a timestamp. */
    public List<PosTicketCommentDto> getPosTicketComments(UUID endpointId, String apiKey, String posTicketId, Instant since) {
        IntegrationEndpoint endpoint = resolveAndValidateApiKey(endpointId, apiKey);
        return ticketService.getPublicCommentsSince(posTicketId, endpoint.getTenantId(), since);
    }

    private IntegrationEndpoint resolveAndValidateApiKey(UUID endpointId, String apiKey) {
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
        return endpoint;
    }

    private void processPosTicket(IntegrationEndpoint endpoint, Map<String, Object> payload) {
        String posTicketId = (String) payload.get("posTicketId");
        try {
            String title       = (String) payload.getOrDefault("title", "Support ticket from POS");
            String description = (String) payload.getOrDefault("description", "");
            String priorityStr = (String) payload.getOrDefault("priority", "MEDIUM");

            Ticket.Priority priority;
            try {
                priority = Ticket.Priority.valueOf(priorityStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                priority = Ticket.Priority.MEDIUM;
            }

            // Do NOT map POS branchId to CT's branch_id FK column —
            // POS branch UUIDs don't exist in client_branches and would cause a FK violation.
            // The full branch info (name, id) is already preserved in posContext (payload).
            ticketService.createFromPosEvent(
                    endpoint.getTenantId(),
                    posTicketId,
                    title,
                    description,
                    priority,
                    null,   // branchId — intentionally null for POS tickets
                    payload
            );
        } catch (Exception e) {
            log.error("processPosTicket FAILED for posTicketId={}: [{}] {}",
                    posTicketId, e.getClass().getSimpleName(), e.getMessage(), e);
            throw new com.controltower.app.shared.exception.ControlTowerException(
                    "POS ticket creation failed: " + e.getMessage(),
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private void processPosChat(IntegrationEndpoint endpoint, Map<String, Object> payload) {
        try {
            String posTicketId = (String) payload.get("posTicketId");
            String content     = (String) payload.getOrDefault("content", "");
            String senderName  = (String) payload.getOrDefault("senderName", "POS User");
            String branchName  = (String) payload.getOrDefault("branchName", "");

            String fullContent = "[" + senderName + "]: " + content;
            ticketService.addExternalComment(posTicketId, endpoint.getTenantId(), fullContent);

            // Find the ticket ID for the event (best-effort — ignore if not found)
            try {
                PosTicketStatusResponse status = ticketService.getStatusForPosTicket(posTicketId, endpoint.getTenantId());
                publisher.publishEvent(new PosTicketChatEvent(
                        endpoint.getTenantId(),
                        status.getCtTicketId(),
                        posTicketId,
                        senderName,
                        content,
                        branchName
                ));
            } catch (Exception ignored) {}
        } catch (Exception e) {
            log.error("Failed to process POS chat message: {}", e.getMessage(), e);
        }
    }

    private IntegrationEndpoint resolve(UUID endpointId) {
        return endpointRepository.findByIdAndTenantIdAndDeletedAtIsNull(
                        endpointId, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("IntegrationEndpoint", endpointId));
    }
}
