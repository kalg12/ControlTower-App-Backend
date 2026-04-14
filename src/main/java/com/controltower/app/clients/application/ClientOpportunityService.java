package com.controltower.app.clients.application;

import com.controltower.app.clients.api.dto.ClientOpportunityRequest;
import com.controltower.app.clients.api.dto.ClientOpportunityResponse;
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

@Service
@RequiredArgsConstructor
public class ClientOpportunityService {

    private final ClientOpportunityRepository opportunityRepository;
    private final ClientRepository            clientRepository;

    @Transactional(readOnly = true)
    public Page<ClientOpportunityResponse> listByClient(UUID clientId, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        return opportunityRepository.findByClientIdAndTenantIdOrderByCreatedAtDesc(clientId, tenantId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ClientOpportunityResponse> listAll(Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        return opportunityRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ClientOpportunityResponse> getActivePipeline() {
        UUID tenantId = TenantContext.getTenantId();
        var activeStages = List.of(
                ClientOpportunity.OpportunityStage.PROSPECTING,
                ClientOpportunity.OpportunityStage.QUALIFIED,
                ClientOpportunity.OpportunityStage.DEMO_SCHEDULED,
                ClientOpportunity.OpportunityStage.PROPOSAL_SENT,
                ClientOpportunity.OpportunityStage.NEGOTIATION,
                ClientOpportunity.OpportunityStage.VERBAL_COMMIT
        );
        return opportunityRepository.findActivePipelineByTenantId(activeStages, tenantId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public ClientOpportunityResponse create(UUID clientId, ClientOpportunityRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        Client client = clientRepository.findByIdAndTenantIdAndDeletedAtIsNull(clientId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", clientId));

        ClientOpportunity opp = new ClientOpportunity();
        opp.setTenant(client.getTenant());
        opp.setClient(client);
        opp.setTitle(request.getTitle());
        opp.setDescription(request.getDescription());
        opp.setValue(request.getValue());
        opp.setCurrency(request.getCurrency() != null ? request.getCurrency() : "MXN");
        opp.setStage(request.getStage());
        opp.setProbability(request.getProbability() != null ? request.getProbability() : request.getStage().getDefaultProbability());
        opp.setOwnerId(request.getOwnerId());
        opp.setExpectedCloseDate(request.getExpectedCloseDate());
        opp.setSource(request.getSource());

        return toResponse(opportunityRepository.save(opp));
    }

    @Transactional
    public ClientOpportunityResponse update(UUID oppId, ClientOpportunityRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        ClientOpportunity opp = opportunityRepository.findByIdAndTenantId(oppId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientOpportunity", oppId));

        opp.setTitle(request.getTitle());
        opp.setDescription(request.getDescription());
        if (request.getValue() != null) opp.setValue(request.getValue());
        if (request.getCurrency() != null) opp.setCurrency(request.getCurrency());
        if (request.getOwnerId() != null) opp.setOwnerId(request.getOwnerId());
        if (request.getExpectedCloseDate() != null) opp.setExpectedCloseDate(request.getExpectedCloseDate());

        // Handle stage change
        if (request.getStage() != null && request.getStage() != opp.getStage()) {
            opp.setStage(request.getStage());
            opp.setProbability(request.getProbability() != null ? request.getProbability() : request.getStage().getDefaultProbability());

            if (request.getStage() == ClientOpportunity.OpportunityStage.CLOSED_WON) {
                opp.setClosedDate(Instant.now());
            } else if (request.getStage() == ClientOpportunity.OpportunityStage.CLOSED_LOST) {
                opp.setClosedDate(Instant.now());
                if (request.getLossReason() != null) opp.setLossReason(request.getLossReason());
            }
        }

        if (request.getLossReason() != null) opp.setLossReason(request.getLossReason());

        return toResponse(opportunityRepository.save(opp));
    }

    @Transactional
    public void delete(UUID oppId) {
        UUID tenantId = TenantContext.getTenantId();
        ClientOpportunity opp = opportunityRepository.findByIdAndTenantId(oppId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientOpportunity", oppId));
        opp.softDelete();
        opportunityRepository.save(opp);
    }

    private ClientOpportunityResponse toResponse(ClientOpportunity co) {
        return ClientOpportunityResponse.builder()
                .id(co.getId())
                .clientId(co.getClient().getId())
                .clientName(co.getClient().getName())
                .branchId(co.getBranchId())
                .title(co.getTitle())
                .description(co.getDescription())
                .value(co.getValue())
                .currency(co.getCurrency())
                .stage(co.getStage().name())
                .probability(co.getProbability())
                .ownerId(co.getOwnerId())
                .ownerName(co.getOwnerName())
                .expectedCloseDate(co.getExpectedCloseDate())
                .closedDate(co.getClosedDate())
                .lossReason(co.getLossReason())
                .source(co.getSource() != null ? co.getSource().name() : null)
                .createdAt(co.getCreatedAt())
                .build();
    }
}
