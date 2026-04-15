package com.controltower.app.time.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SlaNotificationSentRepository extends JpaRepository<SlaNotificationSent, UUID> {

    boolean existsByTicketIdAndThreshold(UUID ticketId, short threshold);

    void deleteByTicketId(UUID ticketId);
}
