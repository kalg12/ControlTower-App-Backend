package com.controltower.app.csat.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CsatSurveyRepository extends JpaRepository<CsatSurvey, UUID> {

    Optional<CsatSurvey> findByToken(UUID token);

    Optional<CsatSurvey> findByTicketIdAndTenantId(UUID ticketId, UUID tenantId);

    Page<CsatSurvey> findByTenantId(UUID tenantId, Pageable pageable);

    List<CsatSurvey> findByTicketId(UUID ticketId);
}
