package com.controltower.app.clients.application;

import com.controltower.app.clients.api.dto.ClientInteractionRequest;
import com.controltower.app.clients.api.dto.ClientInteractionResponse;
import com.controltower.app.clients.domain.*;
import com.controltower.app.identity.domain.Tenant;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientInteractionService {

    private final ClientInteractionRepository interactionRepository;
    private final ClientRepository            clientRepository;
    private final ClientBranchRepository      branchRepository;

    @Transactional(readOnly = true)
    public Page<ClientInteractionResponse> listByClient(UUID clientId, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        return interactionRepository.findByClientIdAndTenantIdOrderByOccurredAtDesc(clientId, tenantId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ClientInteractionResponse> listAllByClient(UUID clientId) {
        UUID tenantId = TenantContext.getTenantId();
        return interactionRepository.findByClientIdAndTenantIdOrderByOccurredAtDesc(clientId, tenantId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ClientInteractionResponse create(UUID clientId, ClientInteractionRequest request, UUID userId, String userName) {
        UUID tenantId = TenantContext.getTenantId();
        Client client = clientRepository.findByIdAndTenantIdAndDeletedAtIsNull(clientId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", clientId));

        // Verify branch belongs to client if provided
        if (request.getBranchId() != null) {
            branchRepository.findByIdAndTenantIdAndDeletedAtIsNull(request.getBranchId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("ClientBranch", request.getBranchId()));
        }

        ClientInteraction interaction = new ClientInteraction();
        interaction.setTenant(client.getTenant());
        interaction.setClient(client);
        interaction.setBranchId(request.getBranchId());
        interaction.setUserId(userId);
        interaction.setUserName(userName);
        interaction.setInteractionType(request.getInteractionType());
        interaction.setTitle(request.getTitle());
        interaction.setDescription(request.getDescription());
        interaction.setOccurredAt(request.getOccurredAt() != null ? request.getOccurredAt() : Instant.now());
        interaction.setTicketId(request.getTicketId());
        interaction.setOutcome(request.getOutcome());
        interaction.setDurationMinutes(request.getDurationMinutes());

        return toResponse(interactionRepository.save(interaction));
    }

    @Transactional
    public void delete(UUID interactionId) {
        UUID tenantId = TenantContext.getTenantId();
        ClientInteraction interaction = interactionRepository.findByIdAndTenantId(interactionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientInteraction", interactionId));
        interaction.softDelete();
        interactionRepository.save(interaction);
    }

    private ClientInteractionResponse toResponse(ClientInteraction ci) {
        String branchName = null;
        if (ci.getBranchId() != null) {
            branchName = branchRepository.findById(ci.getBranchId())
                    .map(ClientBranch::getName).orElse(null);
        }
        return ClientInteractionResponse.builder()
                .id(ci.getId())
                .clientId(ci.getClient().getId())
                .branchId(ci.getBranchId())
                .branchName(branchName)
                .userId(ci.getUserId())
                .userName(ci.getUserName())
                .interactionType(ci.getInteractionType().name())
                .title(ci.getTitle())
                .description(ci.getDescription())
                .occurredAt(ci.getOccurredAt())
                .ticketId(ci.getTicketId())
                .outcome(ci.getOutcome())
                .durationMinutes(ci.getDurationMinutes())
                .createdAt(ci.getCreatedAt())
                .build();
    }
}
