package com.controltower.app.proposals.application;

import com.controltower.app.audit.application.AuditService;
import com.controltower.app.audit.domain.AuditAction;
import com.controltower.app.clients.domain.Client;
import com.controltower.app.clients.domain.ClientContact;
import com.controltower.app.clients.domain.ClientContactRepository;
import com.controltower.app.clients.domain.ClientRepository;
import com.controltower.app.proposals.api.dto.*;
import com.controltower.app.proposals.domain.*;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.shared.infrastructure.EmailService;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final ClientRepository clientRepository;
    private final ClientContactRepository clientContactRepository;
    private final AuditService auditService;
    private final EmailService emailService;

    // ── CREATE / UPDATE / DELETE ─────────────────────────────────────────────

    @Transactional
    public ProposalResponse createProposal(ProposalRequest req) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId   = currentUserId();

        Client client = requireClient(req.clientId());

        Proposal proposal = new Proposal();
        proposal.setTenantId(tenantId);
        proposal.setClientId(req.clientId());
        proposal.setNumber(generateNumber(tenantId));
        proposal.setTitle(req.title());
        proposal.setDescription(req.description());
        proposal.setCurrency(req.currency() != null ? req.currency() : "MXN");
        if (req.taxRate() != null) proposal.setTaxRate(req.taxRate());
        proposal.setValidityDate(req.validityDate());
        proposal.setNotes(req.notes());
        proposal.setTerms(req.terms());

        setLineItems(proposal, req.lineItems());
        proposal.recalculate();

        Proposal saved = proposalRepository.save(proposal);
        auditService.log(AuditAction.PROPOSAL_CREATED, tenantId, userId, "Proposal", saved.getId().toString());
        return toResponse(saved, client);
    }

    @Transactional
    public ProposalResponse updateProposal(UUID id, ProposalRequest req) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId   = currentUserId();

        Proposal proposal = requireProposal(id, tenantId);
        if (proposal.getStatus() != ProposalStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT proposals can be updated");
        }

        Client client = requireClient(req.clientId());

        proposal.setClientId(req.clientId());
        proposal.setTitle(req.title());
        proposal.setDescription(req.description());
        if (req.currency() != null) proposal.setCurrency(req.currency());
        if (req.taxRate() != null) proposal.setTaxRate(req.taxRate());
        proposal.setValidityDate(req.validityDate());
        proposal.setNotes(req.notes());
        proposal.setTerms(req.terms());

        setLineItems(proposal, req.lineItems());
        proposal.recalculate();

        Proposal saved = proposalRepository.save(proposal);
        auditService.log(AuditAction.PROPOSAL_UPDATED, tenantId, userId, "Proposal", id.toString());
        return toResponse(saved, client);
    }

    @Transactional
    public void deleteProposal(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId   = currentUserId();

        Proposal proposal = requireProposal(id, tenantId);
        if (proposal.getStatus() != ProposalStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT proposals can be deleted");
        }
        proposal.softDelete();
        proposalRepository.save(proposal);
        auditService.log(AuditAction.PROPOSAL_DELETED, tenantId, userId, "Proposal", id.toString());
    }

    // ── READ ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ProposalResponse getProposal(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        Proposal proposal = requireProposal(id, tenantId);
        Client client = clientRepository.findById(proposal.getClientId()).orElse(null);
        return toResponse(proposal, client);
    }

    @Transactional(readOnly = true)
    public Page<ProposalResponse> listProposals(ProposalStatus status, UUID clientId,
                                                 Instant from, Instant to, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        Page<Proposal> page = proposalRepository.findFiltered(tenantId, status, clientId, from, to, pageable);
        Map<UUID, Client> clients = loadClients(page.stream()
                .map(Proposal::getClientId).collect(Collectors.toList()));
        return page.map(p -> toResponse(p, clients.get(p.getClientId())));
    }

    @Transactional(readOnly = true)
    public Page<ProposalResponse> listByClient(UUID clientId, ProposalStatus status, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        Page<Proposal> page = proposalRepository.findByClientId(tenantId, clientId, status, pageable);
        Client client = clientRepository.findById(clientId).orElse(null);
        return page.map(p -> toResponse(p, client));
    }

    // ── STATUS TRANSITIONS ───────────────────────────────────────────────────

    @Transactional
    public ProposalResponse sendProposal(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId   = currentUserId();

        Proposal proposal = requireProposal(id, tenantId);
        if (proposal.getStatus() != ProposalStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT proposals can be sent");
        }

        String clientEmail = resolveClientEmail(proposal.getClientId());
        if (clientEmail == null) {
            throw new IllegalStateException("Client has no email address configured");
        }

        Client client = requireClient(proposal.getClientId());

        proposal.setStatus(ProposalStatus.SENT);
        proposal.setSentAt(Instant.now());
        proposal.setSentById(userId);
        Proposal saved = proposalRepository.save(proposal);

        String validUntil = proposal.getValidityDate() != null
                ? proposal.getValidityDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                : "N/A";
        String total = String.format("$%,.2f %s", proposal.getTotal(), proposal.getCurrency());

        emailService.sendProposal(clientEmail, client.getName(),
                proposal.getNumber(), proposal.getTitle(), total, validUntil, "");

        auditService.log(AuditAction.PROPOSAL_SENT, tenantId, userId, "Proposal", id.toString());
        return toResponse(saved, client);
    }

    @Transactional
    public ProposalResponse markViewed(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId   = currentUserId();

        Proposal proposal = requireProposal(id, tenantId);
        if (proposal.getStatus() != ProposalStatus.SENT) {
            throw new IllegalStateException("Only SENT proposals can be marked as viewed");
        }
        proposal.setStatus(ProposalStatus.VIEWED);
        proposal.setViewedAt(Instant.now());
        Proposal saved = proposalRepository.save(proposal);
        auditService.log(AuditAction.UPDATE, tenantId, userId, "Proposal", id.toString());
        return toResponse(saved, clientRepository.findById(proposal.getClientId()).orElse(null));
    }

    @Transactional
    public ProposalResponse acceptProposal(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId   = currentUserId();

        Proposal proposal = requireProposal(id, tenantId);
        if (proposal.getStatus() != ProposalStatus.SENT && proposal.getStatus() != ProposalStatus.VIEWED) {
            throw new IllegalStateException("Only SENT or VIEWED proposals can be accepted");
        }
        proposal.setStatus(ProposalStatus.ACCEPTED);
        proposal.setAcceptedAt(Instant.now());
        Proposal saved = proposalRepository.save(proposal);
        auditService.log(AuditAction.PROPOSAL_ACCEPTED, tenantId, userId, "Proposal", id.toString());
        return toResponse(saved, clientRepository.findById(proposal.getClientId()).orElse(null));
    }

    @Transactional
    public ProposalResponse rejectProposal(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId   = currentUserId();

        Proposal proposal = requireProposal(id, tenantId);
        if (proposal.getStatus() != ProposalStatus.SENT && proposal.getStatus() != ProposalStatus.VIEWED) {
            throw new IllegalStateException("Only SENT or VIEWED proposals can be rejected");
        }
        proposal.setStatus(ProposalStatus.REJECTED);
        proposal.setRejectedAt(Instant.now());
        Proposal saved = proposalRepository.save(proposal);
        auditService.log(AuditAction.PROPOSAL_REJECTED, tenantId, userId, "Proposal", id.toString());
        return toResponse(saved, clientRepository.findById(proposal.getClientId()).orElse(null));
    }

    // ── HELPERS ─────────────────────────────────────────────────────────────

    private Proposal requireProposal(UUID id, UUID tenantId) {
        return proposalRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found: " + id));
    }

    private Client requireClient(UUID clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + clientId));
    }

    private String generateNumber(UUID tenantId) {
        int year = LocalDate.now().getYear();
        long count = proposalRepository.countByTenantIdAndYear(tenantId, year);
        return String.format("PRO-%d-%04d", year, count + 1);
    }

    private String resolveClientEmail(UUID clientId) {
        Client client = clientRepository.findById(clientId).orElse(null);
        if (client != null && client.getPrimaryEmail() != null && !client.getPrimaryEmail().isBlank()) {
            return client.getPrimaryEmail();
        }
        return clientContactRepository.findByClientIdOrderByPrimaryDescCreatedAtAsc(clientId)
                .stream()
                .filter(c -> c.getEmail() != null && !c.getEmail().isBlank())
                .map(ClientContact::getEmail)
                .findFirst()
                .orElse(null);
    }

    private void setLineItems(Proposal proposal, List<ProposalLineItemRequest> reqs) {
        proposal.getLineItems().clear();
        int pos = 0;
        for (ProposalLineItemRequest r : reqs) {
            ProposalLineItem item = new ProposalLineItem();
            item.setProposal(proposal);
            item.setDescription(r.description());
            item.setQuantity(r.quantity());
            item.setUnitPrice(r.unitPrice());
            item.setPosition(r.position() > 0 ? r.position() : pos);
            item.computeSubtotal();
            proposal.getLineItems().add(item);
            pos++;
        }
    }

    private Map<UUID, Client> loadClients(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        return clientRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Client::getId, c -> c));
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        try {
            return UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    ProposalResponse toResponse(Proposal p, Client client) {
        List<ProposalLineItemResponse> items = p.getLineItems().stream()
                .map(li -> new ProposalLineItemResponse(
                        li.getId(), li.getDescription(),
                        li.getQuantity(), li.getUnitPrice(), li.getSubtotal(),
                        li.getPosition(), li.getCreatedAt()))
                .collect(Collectors.toList());

        return ProposalResponse.builder()
                .id(p.getId())
                .tenantId(p.getTenantId())
                .clientId(p.getClientId())
                .clientName(client != null ? client.getName() : null)
                .clientEmail(client != null ? client.getPrimaryEmail() : null)
                .number(p.getNumber())
                .title(p.getTitle())
                .description(p.getDescription())
                .status(p.getStatus())
                .subtotal(p.getSubtotal())
                .taxRate(p.getTaxRate())
                .taxAmount(p.getTaxAmount())
                .total(p.getTotal())
                .currency(p.getCurrency())
                .validityDate(p.getValidityDate())
                .notes(p.getNotes())
                .terms(p.getTerms())
                .sentAt(p.getSentAt())
                .viewedAt(p.getViewedAt())
                .acceptedAt(p.getAcceptedAt())
                .rejectedAt(p.getRejectedAt())
                .sentById(p.getSentById())
                .lineItems(items)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
