package com.controltower.app.campaigns.application;

import com.controltower.app.campaigns.api.dto.CampaignRequest;
import com.controltower.app.campaigns.api.dto.CampaignResponse;
import com.controltower.app.campaigns.api.dto.CampaignUpdateRequest;
import com.controltower.app.campaigns.domain.Campaign;
import com.controltower.app.campaigns.domain.CampaignRepository;
import com.controltower.app.shared.events.UserActionEvent;
import com.controltower.app.shared.exception.ControlTowerException;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final ApplicationEventPublisher publisher;

    @Transactional(readOnly = true)
    public Page<CampaignResponse> list(String search, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        return campaignRepository.findAll(buildListSpec(tenantId, search), pageable)
                .map(this::toResponse);
    }

    @Transactional
    public CampaignResponse create(CampaignRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        Campaign campaign = new Campaign();
        campaign.setTenantId(tenantId);
        campaign.setName(request.getName());
        campaign.setType(request.getType());
        campaign.setSubject(request.getSubject());
        campaign.setBody(request.getBody());
        campaign.setTargetAudience(request.getTargetAudience());
        Instant scheduledAt = parseScheduledAt(request.getScheduledAt());
        if (scheduledAt != null) {
            campaign.setScheduledAt(scheduledAt);
            campaign.setStatus(Campaign.CampaignStatus.SCHEDULED);
        }
        Campaign saved = campaignRepository.save(campaign);
        publisher.publishEvent(UserActionEvent.builder()
                .tenantId(tenantId)
                .userId(resolveUserId())
                .actionName("CAMPAIGN_CREATED")
                .entityType("Campaign")
                .entityId(saved.getId())
                .description("Created campaign '" + saved.getName() + "'")
                .build());
        return toResponse(saved);
    }

    @Transactional
    public CampaignResponse update(UUID id, CampaignUpdateRequest request) {
        Campaign campaign = resolve(id);
        if (campaign.getStatus() == Campaign.CampaignStatus.SENT) {
            throw new ControlTowerException("Cannot edit a sent campaign", HttpStatus.BAD_REQUEST);
        }
        if (request.getName() != null)           campaign.setName(request.getName());
        if (request.getSubject() != null)        campaign.setSubject(request.getSubject());
        if (request.getBody() != null)           campaign.setBody(request.getBody());
        if (request.getTargetAudience() != null) campaign.setTargetAudience(request.getTargetAudience());
        Instant scheduledAt = parseScheduledAt(request.getScheduledAt());
        if (scheduledAt != null) {
            campaign.setScheduledAt(scheduledAt);
            campaign.setStatus(Campaign.CampaignStatus.SCHEDULED);
        }
        return toResponse(campaignRepository.save(campaign));
    }

    @Transactional
    public void send(UUID id) {
        Campaign campaign = resolve(id);
        if (campaign.getStatus() == Campaign.CampaignStatus.SENT) {
            throw new ControlTowerException("Campaign already sent", HttpStatus.BAD_REQUEST);
        }
        campaign.setStatus(Campaign.CampaignStatus.SENT);
        campaign.setSentAt(Instant.now());
        campaignRepository.save(campaign);
        publisher.publishEvent(UserActionEvent.builder()
                .tenantId(TenantContext.getTenantId())
                .userId(resolveUserId())
                .actionName("CAMPAIGN_SENT")
                .entityType("Campaign")
                .entityId(id)
                .description("Sent campaign '" + campaign.getName() + "'")
                .build());
    }

    @Transactional
    public void delete(UUID id) {
        Campaign campaign = resolve(id);
        String campaignName = campaign.getName();
        campaign.softDelete();
        campaignRepository.save(campaign);
        publisher.publishEvent(UserActionEvent.builder()
                .tenantId(TenantContext.getTenantId())
                .userId(resolveUserId())
                .actionName("CAMPAIGN_DELETED")
                .entityType("Campaign")
                .entityId(id)
                .description("Deleted campaign '" + campaignName + "'")
                .build());
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private UUID resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            try { return UUID.fromString(auth.getName()); }
            catch (IllegalArgumentException ignored) {}
        }
        return null;
    }

    private Specification<Campaign> buildListSpec(UUID tenantId, String search) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (search != null && !search.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("name")),
                        "%" + search.trim().toLowerCase() + "%"
                ));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private Campaign resolve(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return campaignRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", id));
    }

    private Instant parseScheduledAt(String scheduledAtRaw) {
        if (scheduledAtRaw == null || scheduledAtRaw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(scheduledAtRaw.trim());
        } catch (DateTimeParseException ex) {
            throw new ControlTowerException(
                    "Invalid scheduledAt format. Use ISO-8601 instant (e.g. 2026-04-04T10:15:30Z)",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private CampaignResponse toResponse(Campaign c) {
        return CampaignResponse.builder()
                .id(c.getId())
                .tenantId(c.getTenantId())
                .name(c.getName())
                .type(c.getType().name())
                .status(c.getStatus().name())
                .subject(c.getSubject())
                .body(c.getBody())
                .targetAudience(c.getTargetAudience())
                .sentCount(c.getSentCount())
                .openRate(c.getOpenRate())
                .scheduledAt(c.getScheduledAt())
                .sentAt(c.getSentAt())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
