package com.controltower.app.integrations.application;

import com.controltower.app.clients.domain.ClientBranch;
import com.controltower.app.clients.domain.ClientBranchRepository;
import com.controltower.app.integrations.api.dto.IntegrationEndpointRequest;
import com.controltower.app.integrations.api.dto.IntegrationEndpointResponse;
import com.controltower.app.integrations.api.dto.IntegrationEventDto;
import com.controltower.app.integrations.api.dto.PosTicketCommentDto;
import com.controltower.app.integrations.api.dto.PosTicketStatusResponse;
import com.controltower.app.integrations.api.dto.WebhookDeliveryDto;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationService {

    private static final String POS_TICKET_EVENT = "POS_SUPPORT_TICKET";

    private final IntegrationEndpointRepository endpointRepository;
    private final IntegrationEventRepository    eventRepository;
    private final WebhookDeliveryRepository     webhookDeliveryRepository;
    private final ApplicationEventPublisher     publisher;
    private final AesEncryptor                  aesEncryptor;
    private final TicketService                 ticketService;
    private final ClientBranchRepository        clientBranchRepository;

    @Transactional(readOnly = true)
    public Page<IntegrationEndpointResponse> listEndpoints(Pageable pageable, String type) {
        UUID tenantId = TenantContext.getTenantId();
        Page<IntegrationEndpoint> page;
        if (type != null && !type.isBlank()) {
            IntegrationEndpoint.EndpointType endpointType =
                    IntegrationEndpoint.EndpointType.valueOf(type.toUpperCase());
            page = endpointRepository.findByTenantIdAndTypeAndDeletedAtIsNull(tenantId, endpointType, pageable);
        } else {
            page = endpointRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable);
        }
        return enrichPage(page, pageable);
    }

    public record IntegrationCreateResult(IntegrationEndpointResponse endpoint, String plainApiKey) {}

    @Transactional
    public IntegrationCreateResult register(IntegrationEndpointRequest request) {
        String plainKey = UUID.randomUUID().toString().replace("-", "");
        IntegrationEndpoint endpoint = new IntegrationEndpoint();
        endpoint.setTenantId(TenantContext.getTenantId());
        endpoint.setClientBranchId(request.getClientBranchId());
        endpoint.setName(request.getName());
        endpoint.setType(request.getType());
        endpoint.setPullUrl(request.getPullUrl());
        endpoint.setApiKey(aesEncryptor.encrypt(plainKey));
        endpoint.setHeartbeatIntervalSeconds(request.getHeartbeatIntervalSeconds());
        endpoint.setContractVersion(request.getContractVersion());
        endpoint.setMetadata(request.getMetadata());
        return new IntegrationCreateResult(toResponse(endpointRepository.save(endpoint)), plainKey);
    }

    @Transactional
    public IntegrationEndpointResponse update(UUID endpointId, IntegrationEndpointRequest request) {
        IntegrationEndpoint endpoint = resolve(endpointId);
        endpoint.setName(request.getName());
        endpoint.setClientBranchId(request.getClientBranchId());
        endpoint.setPullUrl(request.getPullUrl());
        if (request.getApiKey() != null && !request.getApiKey().isBlank()) {
            endpoint.setApiKey(aesEncryptor.encrypt(request.getApiKey()));
        }
        endpoint.setHeartbeatIntervalSeconds(request.getHeartbeatIntervalSeconds());
        endpoint.setContractVersion(request.getContractVersion());
        endpoint.setMetadata(request.getMetadata());
        return toResponse(endpointRepository.save(endpoint));
    }

    /** Returns the decrypted API key for comparison. */
    public String getDecryptedApiKey(IntegrationEndpoint endpoint) {
        return aesEncryptor.decrypt(endpoint.getApiKey());
    }

    @Transactional
    public IntegrationEndpointResponse activate(UUID endpointId) {
        IntegrationEndpoint endpoint = resolve(endpointId);
        endpoint.setActive(true);
        return toResponse(endpointRepository.save(endpoint));
    }

    private Page<IntegrationEndpointResponse> enrichPage(Page<IntegrationEndpoint> page, Pageable pageable) {
        List<UUID> branchIds = page.getContent().stream()
                .map(IntegrationEndpoint::getClientBranchId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<UUID, ClientBranch> branchMap = branchIds.isEmpty() ? Map.of()
                : clientBranchRepository.findAllByIdsWithClient(branchIds).stream()
                    .collect(Collectors.toMap(b -> b.getId(), b -> b));
        List<IntegrationEndpointResponse> content = page.getContent().stream()
                .map(ep -> toResponse(ep, branchMap.get(ep.getClientBranchId())))
                .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    public IntegrationEndpointResponse toResponse(IntegrationEndpoint ep) {
        ClientBranch branch = ep.getClientBranchId() != null
                ? clientBranchRepository.findById(ep.getClientBranchId()).orElse(null)
                : null;
        return toResponse(ep, branch);
    }

    private IntegrationEndpointResponse toResponse(IntegrationEndpoint ep, ClientBranch branch) {
        return IntegrationEndpointResponse.builder()
                .id(ep.getId())
                .tenantId(ep.getTenantId())
                .clientBranchId(ep.getClientBranchId())
                .clientId(branch != null ? branch.getClient().getId() : null)
                .clientName(branch != null ? branch.getClient().getName() : null)
                .branchName(branch != null ? branch.getName() : null)
                .branchSlug(branch != null ? branch.getSlug() : null)
                .name(ep.getName())
                .type(ep.getType())
                .pullUrl(ep.getPullUrl())
                .heartbeatIntervalSeconds(ep.getHeartbeatIntervalSeconds())
                .contractVersion(ep.getContractVersion())
                .metadata(ep.getMetadata())
                .active(ep.isActive())
                .createdAt(ep.getCreatedAt())
                .updatedAt(ep.getUpdatedAt())
                .build();
    }

    @Transactional
    public void deactivate(UUID endpointId) {
        IntegrationEndpoint endpoint = resolve(endpointId);
        endpoint.setActive(false);
        endpointRepository.save(endpoint);
    }

    @Transactional
    public void delete(UUID endpointId) {
        IntegrationEndpoint endpoint = resolve(endpointId);
        endpoint.softDelete();
        endpointRepository.save(endpoint);
    }

    @Transactional(readOnly = true)
    public Page<IntegrationEventDto> getEvents(UUID endpointId, Pageable pageable) {
        resolve(endpointId);
        return eventRepository.findByEndpointIdOrderByReceivedAtDesc(endpointId, pageable)
                .map(e -> IntegrationEventDto.builder()
                        .id(e.getId())
                        .eventType(e.getEventType())
                        .receivedAt(e.getReceivedAt())
                        .processedAt(e.getProcessedAt())
                        .payload(e.getPayload())
                        .build());
    }

    @Transactional(readOnly = true)
    public Page<WebhookDeliveryDto> getWebhookDeliveries(UUID endpointId, Pageable pageable) {
        resolve(endpointId);
        return webhookDeliveryRepository.findByEndpointIdOrderByCreatedAtDesc(endpointId, pageable)
                .map(d -> WebhookDeliveryDto.builder()
                        .id(d.getId())
                        .url(d.getUrl())
                        .status(d.getStatus())
                        .attempts(d.getAttempts())
                        .lastAttemptAt(d.getLastAttemptAt())
                        .responseStatus(d.getResponseStatus())
                        .createdAt(d.getCreatedAt())
                        .build());
    }

    @Transactional
    public String regenerateApiKey(UUID endpointId) {
        IntegrationEndpoint endpoint = resolve(endpointId);
        String newKey = UUID.randomUUID().toString().replace("-", "");
        endpoint.setApiKey(aesEncryptor.encrypt(newKey));
        endpointRepository.save(endpoint);
        return newKey;
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
