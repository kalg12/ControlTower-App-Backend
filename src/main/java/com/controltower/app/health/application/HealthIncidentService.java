package com.controltower.app.health.application;

import com.controltower.app.clients.domain.ClientBranch;
import com.controltower.app.health.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Evaluates health rules after each check and opens/closes incidents accordingly.
 * Publishes HealthIncidentOpenedEvent so other modules can react (e.g., auto-create ticket).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthIncidentService {

    private final HealthCheckRepository      checkRepository;
    private final HealthIncidentRepository   incidentRepository;
    private final HealthRuleRepository       ruleRepository;
    private final ApplicationEventPublisher  eventPublisher;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void evaluateAfterCheck(ClientBranch branch, HealthCheck latestCheck) {
        List<HealthRule> rules = ruleRepository.findActiveRulesForBranch(
            branch.getTenant().getId(), branch.getId()
        );

        boolean shouldOpenIncident = false;
        HealthIncident.Severity maxSeverity = HealthIncident.Severity.MEDIUM;
        String description = "";

        if (rules.isEmpty()) {
            // No rules configured — apply built-in default:
            // open a MEDIUM incident on the very first DOWN check.
            if (latestCheck.getStatus() == HealthCheck.HealthStatus.DOWN) {
                shouldOpenIncident = true;
                description = buildDescription(latestCheck);
            }
        } else {
            for (HealthRule rule : rules) {
                if (rule.getRuleType() == HealthRule.RuleType.CONSECUTIVE_DOWN_CHECKS) {
                    int threshold = rule.getThresholdValue() != null ? rule.getThresholdValue() : 2;
                    List<HealthCheck> recent = checkRepository.findTop10ByBranchIdOrderByCheckedAtDesc(branch.getId());
                    long consecutiveDown = 0;
                    for (HealthCheck c : recent) {
                        if (c.getStatus() == HealthCheck.HealthStatus.DOWN ||
                            c.getStatus() == HealthCheck.HealthStatus.UNKNOWN) {
                            consecutiveDown++;
                        } else {
                            break;
                        }
                    }
                    if (consecutiveDown >= threshold) {
                        shouldOpenIncident = true;
                        description = String.format(
                            "Branch down: %d consecutive unhealthy checks detected", consecutiveDown
                        );
                        if (rule.getSeverity().ordinal() > maxSeverity.ordinal()) {
                            maxSeverity = rule.getSeverity();
                        }
                    }
                }
            }
        }

        Optional<HealthIncident> openIncident =
                incidentRepository.findByBranchIdAndResolvedAtIsNull(branch.getId());

        if (shouldOpenIncident && openIncident.isEmpty()) {
            // Open a new incident
            HealthIncident incident = new HealthIncident();
            incident.setTenantId(branch.getTenant().getId());
            incident.setBranchId(branch.getId());
            incident.setSeverity(maxSeverity);
            incident.setDescription(description);
            incident.setAutoCreated(true);
            incidentRepository.save(incident);

            log.warn("Health incident opened for branch {} (severity={})", branch.getId(), maxSeverity);
            eventPublisher.publishEvent(new HealthIncidentOpenedEvent(incident));

        } else if (!shouldOpenIncident && openIncident.isPresent()
                   && latestCheck.getStatus() == HealthCheck.HealthStatus.UP) {
            // Auto-resolve if back UP
            HealthIncident incident = openIncident.get();
            incident.resolve();
            incidentRepository.save(incident);
            log.info("Health incident {} auto-resolved (branch {} is UP again)", incident.getId(), branch.getId());
        }
    }

    private String buildDescription(HealthCheck check) {
        StringBuilder sb = new StringBuilder("Branch unreachable — pull check failed.");
        if (check.getErrorMessage() != null && !check.getErrorMessage().isBlank()) {
            sb.append(" Error: ").append(check.getErrorMessage());
        }
        return sb.toString();
    }

    @Transactional
    public HealthIncident openManual(
            java.util.UUID tenantId, java.util.UUID branchId,
            HealthIncident.Severity severity, String description) {

        HealthIncident incident = new HealthIncident();
        incident.setTenantId(tenantId);
        incident.setBranchId(branchId);
        incident.setSeverity(severity);
        incident.setDescription(description);
        incident.setAutoCreated(false);
        incidentRepository.save(incident);

        eventPublisher.publishEvent(new HealthIncidentOpenedEvent(incident));
        return incident;
    }

    @Transactional
    public void resolve(java.util.UUID incidentId) {
        HealthIncident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new com.controltower.app.shared.exception.ResourceNotFoundException("HealthIncident", incidentId));
        incident.resolve();
        incidentRepository.save(incident);
    }
}
