package com.controltower.app.csat.application;

import com.controltower.app.csat.domain.CsatSurvey;
import com.controltower.app.csat.domain.CsatSurveyRepository;
import com.controltower.app.identity.domain.UserRepository;
import com.controltower.app.notifications.application.NotificationService;
import com.controltower.app.notifications.domain.Notification;
import com.controltower.app.shared.exception.ControlTowerException;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.support.domain.TicketRepository;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CsatService {

    private final CsatSurveyRepository repo;
    private final TicketRepository     ticketRepository;
    private final NotificationService  notificationService;
    private final UserRepository       userRepository;

    /** Creates a new survey token for a ticket. Idempotent — returns existing if already sent. */
    @Transactional
    public CsatSurvey createOrGet(UUID ticketId) {
        UUID tenantId = TenantContext.getTenantId();
        return repo.findByTicketIdAndTenantId(ticketId, tenantId)
                .orElseGet(() -> {
                    CsatSurvey survey = new CsatSurvey();
                    survey.setTenantId(tenantId);
                    survey.setTicketId(ticketId);
                    return repo.save(survey);
                });
    }

    /** Public endpoint — no auth, uses token. Submit customer rating. */
    @Transactional
    public CsatSurvey submitRating(UUID token, short rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new ControlTowerException("Rating must be between 1 and 5", HttpStatus.BAD_REQUEST);
        }
        CsatSurvey survey = repo.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("CsatSurvey", token));
        if (survey.getRespondedAt() != null) {
            throw new ControlTowerException("Survey already submitted", HttpStatus.CONFLICT);
        }
        survey.setRating(rating);
        survey.setComment(comment);
        survey.setRespondedAt(Instant.now());
        CsatSurvey saved = repo.save(survey);

        sendCsatNotifications(saved, rating);
        return saved;
    }

    private void sendCsatNotifications(CsatSurvey survey, short rating) {
        ticketRepository.findById(survey.getTicketId()).ifPresent(ticket -> {
            List<UUID> recipients = new ArrayList<>();
            if (ticket.getAssigneeId() != null) recipients.add(ticket.getAssigneeId());

            if (!recipients.isEmpty()) {
                notificationService.send(
                        survey.getTenantId(),
                        "CSAT_RESPONSE_RECEIVED",
                        "Respuesta CSAT recibida",
                        "El cliente calificó con " + rating + "/5 el ticket \"" + ticket.getTitle() + "\"",
                        Notification.Severity.INFO,
                        Map.of("ticketId", ticket.getId().toString(), "rating", (int) rating),
                        recipients);
            }

            if (rating <= 2) {
                List<UUID> managers = userRepository.findByTenantIdAndPermission(
                        survey.getTenantId(), "ticket:write").stream()
                        .map(u -> u.getId())
                        .filter(id -> !recipients.contains(id))
                        .collect(java.util.stream.Collectors.toList());

                if (!managers.isEmpty()) {
                    notificationService.send(
                            survey.getTenantId(),
                            "CSAT_LOW_SCORE",
                            "Calificación CSAT baja",
                            "Un cliente calificó " + rating + "/5 el ticket \"" + ticket.getTitle() + "\". Requiere seguimiento.",
                            Notification.Severity.ERROR,
                            Map.of("ticketId", ticket.getId().toString(), "rating", (int) rating),
                            managers);
                }
            }
        });
    }

    /** Preview survey details by token (public, no auth). */
    @Transactional(readOnly = true)
    public CsatSurvey getByToken(UUID token) {
        return repo.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("CsatSurvey", token));
    }

    @Transactional(readOnly = true)
    public List<CsatSurvey> listByTicket(UUID ticketId) {
        return repo.findByTicketId(ticketId);
    }

    @Transactional(readOnly = true)
    public Page<CsatSurvey> listByTenant(Pageable pageable) {
        return repo.findByTenantId(TenantContext.getTenantId(), pageable);
    }
}
