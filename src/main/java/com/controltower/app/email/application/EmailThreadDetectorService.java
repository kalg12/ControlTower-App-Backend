package com.controltower.app.email.application;

import com.controltower.app.email.domain.EmailRaw;
import com.controltower.app.email.domain.EmailRawRepository;
import com.controltower.app.email.domain.EmailTicketLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Detects whether an inbound email belongs to an existing ticket thread.
 * Priority order:
 *   1. Exact match on In-Reply-To header
 *   2. Any message-id in the References header
 *   3. Reply-To parsing: ticket+{id}@... pattern
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailThreadDetectorService {

    private final EmailRawRepository emailRawRepo;
    private final EmailTicketLinkRepository linkRepo;

    public Optional<UUID> findExistingTicket(UUID tenantId, EmailRaw raw) {
        // Strategy 1: In-Reply-To header — most reliable
        if (raw.getInReplyTo() != null) {
            Optional<UUID> ticketId = findTicketByMessageId(tenantId, raw.getInReplyTo());
            if (ticketId.isPresent()) {
                log.debug("Thread detected via In-Reply-To {} → ticket {}", raw.getInReplyTo(), ticketId.get());
                return ticketId;
            }
        }

        // Strategy 2: References header (array of previous message-ids)
        if (raw.getReferencesIds() != null && raw.getReferencesIds().length > 0) {
            List<String> refs = Arrays.asList(raw.getReferencesIds());
            List<UUID> ticketIds = linkRepo.findTicketIdsByMessageIds(tenantId, refs);
            if (!ticketIds.isEmpty()) {
                log.debug("Thread detected via References → ticket {}", ticketIds.get(0));
                return Optional.of(ticketIds.get(0));
            }
        }

        // Strategy 3: Reply-To header with pattern ticket+{uuid}@domain
        if (raw.getReplyTo() != null) {
            Optional<UUID> parsed = parseTicketIdFromReplyTo(raw.getReplyTo());
            if (parsed.isPresent()) {
                log.debug("Thread detected via Reply-To pattern → ticket {}", parsed.get());
                return parsed;
            }
        }

        return Optional.empty();
    }

    private Optional<UUID> findTicketByMessageId(UUID tenantId, String messageId) {
        return emailRawRepo.findByMessageId(messageId)
            .flatMap(email -> {
                List<UUID> ids = linkRepo.findTicketIdsByMessageIds(tenantId, List.of(email.getMessageId()));
                return ids.isEmpty() ? Optional.empty() : Optional.of(ids.get(0));
            });
    }

    /** Parses ticket+{uuid}@... patterns in Reply-To addresses. */
    private Optional<UUID> parseTicketIdFromReplyTo(String replyTo) {
        if (replyTo == null) return Optional.empty();
        String localPart = replyTo.contains("@") ? replyTo.substring(0, replyTo.indexOf('@')) : replyTo;
        if (localPart.startsWith("ticket+")) {
            String uuidStr = localPart.substring("ticket+".length());
            try {
                return Optional.of(UUID.fromString(uuidStr));
            } catch (IllegalArgumentException ignored) {}
        }
        return Optional.empty();
    }
}
