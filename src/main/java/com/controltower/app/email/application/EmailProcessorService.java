package com.controltower.app.email.application;

import com.controltower.app.email.domain.*;
import com.controltower.app.support.domain.Ticket;
import com.controltower.app.support.domain.TicketComment;
import com.controltower.app.support.domain.TicketRepository;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Async orchestrator: parses a MimeMessage, persists EmailRaw,
 * evaluates routing rules, creates or updates a ticket, and saves links.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailProcessorService {

    private final EmailRawRepository emailRawRepo;
    private final EmailAliasRepository aliasRepo;
    private final EmailTicketLinkRepository linkRepo;
    private final TicketRepository ticketRepo;
    private final EmailRuleEngineService ruleEngine;
    private final EmailThreadDetectorService threadDetector;

    @Async
    @Transactional
    public void processAsync(EmailMailboxConfig config, MimeMessage mimeMessage) {
        try {
            String messageId = extractMessageId(mimeMessage);

            if (messageId == null) {
                log.warn("Email without Message-ID received from mailbox {}, skipping", config.getImapUsername());
                return;
            }

            // Idempotency: skip if already processed
            if (emailRawRepo.existsByMessageId(messageId)) {
                log.debug("Duplicate email {} ignored", messageId);
                return;
            }

            EmailRaw raw = parseToEmailRaw(config, mimeMessage, messageId);

            // Resolve alias from To: address
            Optional<EmailAlias> alias = resolveAlias(config.getTenantId(), mimeMessage);
            alias.ifPresent(a -> raw.setAliasId(a.getId()));

            try {
                raw.setStatus(EmailRaw.EmailStatus.PROCESSING);
                emailRawRepo.save(raw);
            } catch (DataIntegrityViolationException e) {
                // Race condition: another poller inserted the same message-id
                log.debug("Duplicate email {} (concurrent insert) ignored", messageId);
                return;
            }

            // Evaluate routing rules
            EmailRuleEngineService.RoutingResult routing = ruleEngine.evaluate(
                config.getTenantId(), raw, alias.orElse(null), config
            );

            // Check if this is part of an existing ticket thread
            Optional<UUID> existingTicketId = threadDetector.findExistingTicket(
                config.getTenantId(), raw
            );

            if (existingTicketId.isPresent()) {
                addMessageToTicket(existingTicketId.get(), raw, routing);
            } else {
                createTicketFromEmail(config.getTenantId(), raw, routing);
            }

            raw.setStatus(EmailRaw.EmailStatus.PROCESSED);
            raw.setProcessedAt(Instant.now());
            emailRawRepo.save(raw);

        } catch (Exception e) {
            log.error("Failed to process email from mailbox {}: {}", config.getImapUsername(), e.getMessage(), e);
            // Best-effort: try to mark as FAILED
            try {
                String messageId = extractMessageId(mimeMessage);
                if (messageId != null) {
                    emailRawRepo.findByMessageId(messageId).ifPresent(raw -> {
                        raw.setStatus(EmailRaw.EmailStatus.FAILED);
                        raw.setErrorMessage(e.getMessage());
                        emailRawRepo.save(raw);
                    });
                }
            } catch (Exception ignored) {}
        }
    }

    private EmailRaw parseToEmailRaw(EmailMailboxConfig config, MimeMessage msg, String messageId)
            throws MessagingException {
        EmailRaw raw = new EmailRaw();
        raw.setTenantId(config.getTenantId());
        raw.setMailboxId(config.getId());
        raw.setMessageId(messageId);
        raw.setInReplyTo(getHeader(msg, "In-Reply-To"));
        raw.setReferencesIds(parseReferences(getHeader(msg, "References")));
        raw.setFromEmail(extractEmail(msg.getFrom()));
        raw.setFromName(extractName(msg.getFrom()));
        raw.setToEmail(extractAddresses(msg.getRecipients(Message.RecipientType.TO)));
        raw.setCcEmail(extractAddresses(msg.getRecipients(Message.RecipientType.CC)));
        raw.setReplyTo(extractEmail(msg.getReplyTo()));
        raw.setSubject(msg.getSubject());
        raw.setReceivedAt(msg.getReceivedDate() != null
            ? msg.getReceivedDate().toInstant()
            : Instant.now());

        // Parse body — best effort
        try {
            EmailBodyParser.Result body = EmailBodyParser.parse(msg);
            raw.setBodyText(body.text());
            raw.setBodyHtml(body.html());
        } catch (Exception e) {
            log.warn("Could not parse body for {}: {}", messageId, e.getMessage());
        }

        // Check for auto-submitted header (prevents auto-reply loops)
        String autoSubmitted = getHeader(msg, "X-Auto-Submitted");
        if (autoSubmitted != null && !autoSubmitted.equalsIgnoreCase("no")) {
            raw.setSpam(true); // treat auto-replies as spam-like to avoid loops
        }

        raw.setStatus(EmailRaw.EmailStatus.RECEIVED);
        return raw;
    }

    private void createTicketFromEmail(UUID tenantId, EmailRaw raw, EmailRuleEngineService.RoutingResult routing) {
        Ticket ticket = new Ticket();
        ticket.setTenantId(tenantId);
        ticket.setTitle(raw.getSubject() != null ? raw.getSubject() : "(Sin asunto)");
        ticket.setDescription(raw.getBodyText());
        ticket.setSource(Ticket.TicketSource.EMAIL);
        ticket.setSourceEmailId(raw.getId());
        ticket.setRequesterEmail(raw.getFromEmail());
        ticket.setPriority(routing.priority());
        ticket.setDepartmentId(routing.departmentId());
        if (routing.assigneeId() != null) {
            ticket.setAssigneeId(routing.assigneeId());
            ticket.setStatus(Ticket.TicketStatus.IN_PROGRESS);
        }
        if (routing.labels() != null) {
            ticket.setLabels(routing.labels());
        }

        ticketRepo.save(ticket);

        EmailTicketLink link = new EmailTicketLink();
        link.setEmailId(raw.getId());
        link.setTicketId(ticket.getId());
        link.setLinkType(EmailTicketLink.LinkType.CREATED_FROM);
        linkRepo.save(link);

        log.info("Ticket {} created from email {} (tenant {})", ticket.getId(), raw.getMessageId(), tenantId);
    }

    private void addMessageToTicket(UUID ticketId, EmailRaw raw, EmailRuleEngineService.RoutingResult routing) {
        Ticket ticket = ticketRepo.findById(ticketId).orElseThrow();

        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setContent(raw.getBodyText() != null ? raw.getBodyText() : "(Sin contenido)");
        comment.setSource("EMAIL");
        comment.setEmailRawId(raw.getId());
        comment.setInternal(false);
        ticket.getComments().add(comment);
        ticketRepo.save(ticket);

        EmailTicketLink link = new EmailTicketLink();
        link.setEmailId(raw.getId());
        link.setTicketId(ticketId);
        link.setLinkType(EmailTicketLink.LinkType.ADDED_MESSAGE);
        linkRepo.save(link);

        log.info("Email {} appended to ticket {} as comment", raw.getMessageId(), ticketId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Optional<EmailAlias> resolveAlias(UUID tenantId, MimeMessage msg) {
        try {
            Address[] toAddresses = msg.getRecipients(Message.RecipientType.TO);
            if (toAddresses == null) return Optional.empty();
            for (Address addr : toAddresses) {
                if (addr instanceof InternetAddress ia) {
                    Optional<EmailAlias> alias = aliasRepo.findByTenantIdAndAlias(tenantId, ia.getAddress().toLowerCase());
                    if (alias.isPresent()) return alias;
                }
            }
        } catch (MessagingException ignored) {}
        return Optional.empty();
    }

    private String extractMessageId(MimeMessage msg) {
        try {
            return msg.getMessageID();
        } catch (MessagingException e) {
            return null;
        }
    }

    private String getHeader(MimeMessage msg, String name) {
        try {
            String[] vals = msg.getHeader(name);
            return (vals != null && vals.length > 0) ? vals[0].trim() : null;
        } catch (MessagingException e) {
            return null;
        }
    }

    private String[] parseReferences(String references) {
        if (references == null || references.isBlank()) return new String[0];
        return Arrays.stream(references.split("\\s+"))
            .filter(s -> !s.isBlank())
            .toArray(String[]::new);
    }

    private String extractEmail(Address[] addresses) {
        if (addresses == null || addresses.length == 0) return null;
        return (addresses[0] instanceof InternetAddress ia) ? ia.getAddress() : null;
    }

    private String extractName(Address[] addresses) {
        if (addresses == null || addresses.length == 0) return null;
        return (addresses[0] instanceof InternetAddress ia) ? ia.getPersonal() : null;
    }

    private String[] extractAddresses(Address[] addresses) {
        if (addresses == null) return new String[0];
        return Arrays.stream(addresses)
            .filter(a -> a instanceof InternetAddress)
            .map(a -> ((InternetAddress) a).getAddress())
            .toArray(String[]::new);
    }
}
