package com.controltower.app.support.application;

import com.controltower.app.clients.domain.ClientRepository;
import com.controltower.app.identity.domain.UserRepository;
import com.controltower.app.notes.domain.NoteRepository;
import com.controltower.app.notifications.application.NotificationService;
import com.controltower.app.shared.infrastructure.EmailService;
import com.controltower.app.support.api.dto.AddCommentRequest;
import com.controltower.app.support.domain.PosTicketWebhookEvent;
import com.controltower.app.support.domain.Ticket;
import com.controltower.app.support.domain.TicketRepository;
import com.controltower.app.support.domain.TicketSlaRepository;
import com.controltower.app.tenancy.domain.TenantContext;
import com.controltower.app.time.application.SlaConfigService;
import com.controltower.app.time.domain.TimeEntryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock TicketRepository ticketRepository;
    @Mock TicketSlaRepository slaRepository;
    @Mock ApplicationEventPublisher publisher;
    @Mock SlaConfigService slaConfigService;
    @Mock UserRepository userRepository;
    @Mock ClientRepository clientRepository;
    @Mock NoteRepository noteRepository;
    @Mock TimeEntryRepository timeEntryRepository;
    @Mock NotificationService notificationService;
    @Mock EmailService emailService;

    @InjectMocks TicketService service;

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void flushesCommentBeforePublishingPosWebhook() {
        UUID tenantId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-08-03T05:00:00Z");
        TenantContext.setTenantId(tenantId);

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setTenantId(tenantId);
        ticket.setTitle("POS printer failure");
        ticket.setSource(Ticket.TicketSource.POS);
        ticket.setSourceRefId("pos-ticket-1");
        ticket.setPosContext(Map.of(
                "callbackUrl", "https://pos.example.com/support/webhooks/ct"
        ));

        when(ticketRepository.findByIdAndTenantIdAndDeletedAtIsNull(ticketId, tenantId))
                .thenReturn(Optional.of(ticket));
        when(ticketRepository.saveAndFlush(ticket)).thenAnswer(invocation -> {
            var comment = ticket.getComments().getFirst();
            comment.setId(commentId);
            comment.setCreatedAt(createdAt);
            return ticket;
        });
        when(userRepository.findById(authorId)).thenReturn(Optional.empty());
        when(userRepository.findByTenantIdAndPermission(tenantId, "ticket:read"))
                .thenReturn(List.of());

        AddCommentRequest request = new AddCommentRequest();
        request.setContent("Restart the printer service");
        request.setInternal(false);

        service.addComment(ticketId, request, authorId);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(publisher).publishEvent(eventCaptor.capture());
        PosTicketWebhookEvent event = (PosTicketWebhookEvent) eventCaptor.getValue();
        assertThat(event.getCommentId()).isEqualTo(commentId);
        assertThat(event.getOccurredAt()).isEqualTo(createdAt);
        assertThat(event.getContent()).isEqualTo("Restart the printer service");
    }
}
