package com.controltower.app.csat.application;

import com.controltower.app.csat.domain.CsatSurvey;
import com.controltower.app.csat.domain.CsatSurveyRepository;
import com.controltower.app.shared.exception.ControlTowerException;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CsatService {

    private final CsatSurveyRepository repo;

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
        return repo.save(survey);
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
