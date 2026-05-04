package com.controltower.app.chat.application;

import com.controltower.app.audit.application.AuditService;
import com.controltower.app.audit.domain.AuditAction;
import com.controltower.app.chat.api.dto.*;
import com.controltower.app.chat.domain.*;
import com.controltower.app.chat.api.dto.OnlineAgentResponse;
import com.controltower.app.chat.api.dto.PublicConversationResponse;
import com.controltower.app.identity.domain.User;
import com.controltower.app.identity.domain.UserRepository;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatTransferRepository transferRepository;
    private final ChatQuickReplyRepository quickReplyRepository;
    private final ChatRatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final SimpMessagingTemplate messagingTemplate;

    // ── Visitor: start conversation ──────────────────────────────────────────

    @Transactional
    public StartChatResponse startConversation(StartChatRequest req) {
        ChatConversation conv = new ChatConversation();
        conv.setTenantId(req.tenantId());
        conv.setVisitorId(req.visitorId() != null ? req.visitorId() : UUID.randomUUID().toString());
        conv.setVisitorToken(UUID.randomUUID());
        conv.setVisitorName(req.visitorName());
        conv.setVisitorEmail(req.visitorEmail());
        conv.setSource(req.source() != null ? req.source() : "POS");
        conv.setStatus(ConversationStatus.WAITING);

        ChatConversation saved = conversationRepository.save(conv);

        // notify agents subscribed to the queue
        messagingTemplate.convertAndSend(
                "/topic/chat.queue." + req.tenantId(),
                toResponse(saved, null, 0));

        auditService.log(AuditAction.CHAT_CONVERSATION_STARTED,
                req.tenantId(), null, "ChatConversation", saved.getId().toString());

        return new StartChatResponse(saved.getId(), saved.getVisitorToken());
    }

    // ── Agent: claim ─────────────────────────────────────────────────────────

    @Transactional
    public ChatConversationResponse claimConversation(UUID id) {
        UUID agentId = currentUserId();
        UUID tenantId = TenantContext.getTenantId();

        ChatConversation conv = requireConversation(id);
        if (conv.getStatus() != ConversationStatus.WAITING) {
            throw new IllegalStateException("Only WAITING conversations can be claimed");
        }

        User agent = userRepository.findByIdAndDeletedAtIsNull(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));

        conv.setAgentId(agentId);
        conv.setStatus(ConversationStatus.ACTIVE);
        conversationRepository.save(conv);

        sendSystemMessage(conv, "Agente " + agent.getFullName() + " tomó la conversación");

        auditService.log(AuditAction.CHAT_CLAIMED, tenantId, agentId,
                "ChatConversation", id.toString());

        ChatConversationResponse response = toResponse(conv, agent, 0);
        broadcast(id, buildStatusChangedPayload(id, ConversationStatus.ACTIVE, agent));
        return response;
    }

    // ── Agent: transfer ──────────────────────────────────────────────────────

    @Transactional
    public ChatConversationResponse transferConversation(UUID id, TransferRequest req) {
        UUID fromAgentId = currentUserId();
        UUID tenantId = TenantContext.getTenantId();

        ChatConversation conv = requireConversation(id);
        if (conv.getStatus() != ConversationStatus.ACTIVE && conv.getStatus() != ConversationStatus.TRANSFERRED) {
            throw new IllegalStateException("Only ACTIVE or TRANSFERRED conversations can be transferred");
        }

        User toAgent = userRepository.findByIdAndDeletedAtIsNull(req.toAgentId())
                .orElseThrow(() -> new ResourceNotFoundException("Target agent not found"));
        User fromAgent = userRepository.findByIdAndDeletedAtIsNull(fromAgentId).orElse(null);

        ChatTransfer transfer = new ChatTransfer();
        transfer.setConversationId(id);
        transfer.setFromAgentId(fromAgentId);
        transfer.setToAgentId(req.toAgentId());
        transfer.setNote(req.note());
        transferRepository.save(transfer);

        conv.setAgentId(req.toAgentId());
        conv.setStatus(ConversationStatus.ACTIVE);
        conversationRepository.save(conv);

        String fromName = fromAgent != null ? fromAgent.getFullName() : "Agente anterior";
        sendSystemMessage(conv, "Conversación transferida de " + fromName + " a " + toAgent.getFullName());

        auditService.log(AuditAction.CHAT_TRANSFERRED, tenantId, fromAgentId,
                "ChatConversation", id.toString());

        broadcast(id, buildStatusChangedPayload(id, ConversationStatus.ACTIVE, toAgent));
        return toResponse(conv, toAgent, 0);
    }

    // ── Agent: close ─────────────────────────────────────────────────────────

    @Transactional
    public ChatConversationResponse closeConversation(UUID id) {
        UUID agentId = currentUserId();
        UUID tenantId = TenantContext.getTenantId();

        ChatConversation conv = requireConversation(id);
        if (conv.getStatus() == ConversationStatus.CLOSED || conv.getStatus() == ConversationStatus.ARCHIVED) {
            throw new IllegalStateException("Conversation is already closed");
        }

        conv.setStatus(ConversationStatus.CLOSED);
        conv.setClosedAt(Instant.now());
        conversationRepository.save(conv);

        sendSystemMessage(conv, "Conversación cerrada");

        auditService.log(AuditAction.CHAT_CLOSED, tenantId, agentId,
                "ChatConversation", id.toString());

        broadcast(id, buildStatusChangedPayload(id, ConversationStatus.CLOSED, null));
        return toResponse(conv, null, 0);
    }

    // ── Agent: archive ───────────────────────────────────────────────────────

    @Transactional
    public void archiveConversation(UUID id) {
        UUID agentId = currentUserId();
        UUID tenantId = TenantContext.getTenantId();

        ChatConversation conv = requireConversation(id);
        if (conv.getStatus() != ConversationStatus.CLOSED) {
            throw new IllegalStateException("Only CLOSED conversations can be archived");
        }

        conv.setStatus(ConversationStatus.ARCHIVED);
        conv.setArchivedAt(Instant.now());
        conversationRepository.save(conv);

        auditService.log(AuditAction.CHAT_ARCHIVED, tenantId, agentId,
                "ChatConversation", id.toString());
    }

    // ── Agent: delete ────────────────────────────────────────────────────────

    @Transactional
    public void deleteConversation(UUID id) {
        UUID agentId = currentUserId();
        UUID tenantId = TenantContext.getTenantId();

        ChatConversation conv = requireConversation(id);
        conv.setDeletedAt(Instant.now());
        conversationRepository.save(conv);

        auditService.log(AuditAction.CHAT_DELETED, tenantId, agentId,
                "ChatConversation", id.toString());
    }

    // ── Send message (shared) ────────────────────────────────────────────────

    @Transactional
    public ChatMessageResponse sendMessage(UUID conversationId, SenderType senderType,
                                           UUID senderId, String content) {
        ChatConversation conv = conversationRepository.findByIdAndDeletedAtIsNull(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));

        ChatMessage msg = new ChatMessage();
        msg.setConversation(conv);
        msg.setSenderType(senderType);
        msg.setSenderId(senderId);
        msg.setContent(content);
        msg.setRead(senderType == SenderType.AGENT);
        ChatMessage saved = messageRepository.save(msg);

        User sender = (senderId != null)
                ? userRepository.findByIdAndDeletedAtIsNull(senderId).orElse(null)
                : null;

        ChatMessageResponse response = toMessageResponse(saved, sender);
        ChatMessagePayload payload = ChatMessagePayload.builder()
                .type("MESSAGE")
                .id(saved.getId())
                .conversationId(conversationId)
                .senderType(senderType)
                .senderId(senderId)
                .senderName(sender != null ? sender.getFullName() : conv.getVisitorName())
                .senderAvatarUrl(sender != null ? sender.getAvatarUrl() : null)
                .content(content)
                .isRead(saved.isRead())
                .createdAt(saved.getCreatedAt())
                .build();

        broadcast(conversationId, payload);
        return response;
    }

    @Transactional
    public ChatMessageResponse sendMessageWithAttachment(UUID conversationId, UUID agentId,
                                                          String attachmentUrl, String filename) {
        ChatConversation conv = conversationRepository.findByIdAndDeletedAtIsNull(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));

        ChatMessage msg = new ChatMessage();
        msg.setConversation(conv);
        msg.setSenderType(SenderType.AGENT);
        msg.setSenderId(agentId);
        msg.setContent(filename != null ? "📎 " + filename : "📎 Adjunto");
        msg.setAttachmentUrl(attachmentUrl);
        msg.setRead(true);
        ChatMessage saved = messageRepository.save(msg);

        User sender = userRepository.findByIdAndDeletedAtIsNull(agentId).orElse(null);
        ChatMessageResponse response = toMessageResponse(saved, sender);

        broadcast(conversationId, ChatMessagePayload.builder()
                .type("MESSAGE")
                .id(saved.getId())
                .conversationId(conversationId)
                .senderType(SenderType.AGENT)
                .senderId(agentId)
                .senderName(sender != null ? sender.getFullName() : null)
                .senderAvatarUrl(sender != null ? sender.getAvatarUrl() : null)
                .content(msg.getContent())
                .attachmentUrl(attachmentUrl)
                .isRead(true)
                .createdAt(saved.getCreatedAt())
                .build());

        return response;
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ChatConversationResponse> listConversations(ConversationStatus status,
                                                             UUID agentId, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        return conversationRepository.findFiltered(tenantId, status, agentId, pageable)
                .map(c -> {
                    User agent = c.getAgentId() != null
                            ? userRepository.findByIdAndDeletedAtIsNull(c.getAgentId()).orElse(null)
                            : null;
                    long unread = messageRepository.countUnreadByConversationId(c.getId());
                    return toResponse(c, agent, unread);
                });
    }

    @Transactional(readOnly = true)
    public ChatConversationResponse getConversation(UUID id) {
        ChatConversation conv = requireConversation(id);
        User agent = conv.getAgentId() != null
                ? userRepository.findByIdAndDeletedAtIsNull(conv.getAgentId()).orElse(null)
                : null;
        List<ChatMessage> msgs = messageRepository.findTop50ByConversationIdOrderByCreatedAtAsc(id);
        List<ChatMessageResponse> messageResponses = msgs.stream()
                .map(m -> {
                    User sender = m.getSenderId() != null
                            ? userRepository.findByIdAndDeletedAtIsNull(m.getSenderId()).orElse(null)
                            : null;
                    return toMessageResponse(m, sender);
                }).toList();

        return ChatConversationResponse.builder()
                .id(conv.getId())
                .tenantId(conv.getTenantId())
                .visitorId(conv.getVisitorId())
                .visitorName(conv.getVisitorName())
                .visitorEmail(conv.getVisitorEmail())
                .agentId(conv.getAgentId())
                .agentName(agent != null ? agent.getFullName() : null)
                .agentAvatarUrl(agent != null ? agent.getAvatarUrl() : null)
                .status(conv.getStatus())
                .source(conv.getSource())
                .unreadCount(0)
                .messages(messageResponses)
                .createdAt(conv.getCreatedAt())
                .updatedAt(conv.getUpdatedAt())
                .closedAt(conv.getClosedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getMessages(UUID conversationId, Pageable pageable) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, pageable)
                .map(m -> {
                    User sender = m.getSenderId() != null
                            ? userRepository.findByIdAndDeletedAtIsNull(m.getSenderId()).orElse(null)
                            : null;
                    return toMessageResponse(m, sender);
                });
    }

    @Transactional
    public void markRead(UUID conversationId) {
        messageRepository.markAllReadByConversationId(conversationId);
    }

    @Transactional(readOnly = true)
    public long countWaiting() {
        UUID tenantId = TenantContext.getTenantId();
        return conversationRepository.countWaiting(tenantId);
    }

    @Transactional(readOnly = true)
    public List<ChatQuickReply> getQuickReplies() {
        UUID tenantId = TenantContext.getTenantId();
        return quickReplyRepository.findByTenantIdOrderByShortcutAsc(tenantId);
    }

    // ── Agent presence ───────────────────────────────────────────────────────

    @Transactional
    public void setPresence(UUID userId, boolean online) {
        userRepository.updateChatOnline(userId, online);
    }

    @Transactional(readOnly = true)
    public boolean getMyPresence(UUID userId) {
        return userRepository.findChatOnlineById(userId).orElse(false);
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getAvailability(UUID tenantId) {
        long count = userRepository.countChatOnlineByTenantId(tenantId);
        return java.util.Map.of("available", count > 0, "agentCount", count);
    }

    // ── Agent: unarchive ─────────────────────────────────────────────────────

    @Transactional
    public ChatConversationResponse unarchiveConversation(UUID id) {
        UUID agentId = currentUserId();
        UUID tenantId = TenantContext.getTenantId();

        ChatConversation conv = requireConversation(id);
        if (conv.getStatus() != ConversationStatus.ARCHIVED) {
            throw new IllegalStateException("Only ARCHIVED conversations can be unarchived");
        }

        conv.setStatus(ConversationStatus.CLOSED);
        conv.setArchivedAt(null);
        conversationRepository.save(conv);

        auditService.log(AuditAction.CHAT_UNARCHIVED, tenantId, agentId,
                "ChatConversation", id.toString());

        return toResponse(conv, null, 0);
    }

    // ── Visitor: rate conversation ────────────────────────────────────────────

    @Transactional
    public void rateConversation(UUID conversationId, UUID visitorToken, int rating, String comment) {
        ChatConversation conv = requireConversationByToken(visitorToken);
        if (!conv.getId().equals(conversationId)) {
            throw new IllegalStateException("Token does not match conversation");
        }
        if (conv.getStatus() != ConversationStatus.CLOSED && conv.getStatus() != ConversationStatus.ARCHIVED) {
            throw new IllegalStateException("Conversation must be closed to rate");
        }
        if (ratingRepository.existsByConversationId(conversationId)) {
            throw new IllegalStateException("Conversation already rated");
        }

        ChatRating r = new ChatRating();
        r.setConversationId(conversationId);
        r.setTenantId(conv.getTenantId());
        r.setRating((short) rating);
        r.setComment(comment);
        ratingRepository.save(r);

        auditService.log(AuditAction.CHAT_RATED, conv.getTenantId(), null,
                "ChatConversation", conversationId.toString());
    }

    // ── Agent: read rating ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<ChatRating> getRating(UUID conversationId) {
        return ratingRepository.findByConversationId(conversationId);
    }

    // ── Public: get conversation status (visitor re-sync after STOMP reconnect)

    @Transactional(readOnly = true)
    public PublicConversationResponse getPublicConversation(UUID conversationId, UUID visitorToken) {
        ChatConversation conv = requireConversationByToken(visitorToken);
        if (!conv.getId().equals(conversationId)) {
            throw new IllegalStateException("Token does not match conversation");
        }
        User agent = (conv.getAgentId() != null)
                ? userRepository.findByIdAndDeletedAtIsNull(conv.getAgentId()).orElse(null)
                : null;
        return new PublicConversationResponse(
                conv.getId(),
                conv.getStatus().name(),
                agent != null ? agent.getFullName() : null,
                agent != null ? agent.getAvatarUrl() : null
        );
    }

    // ── Agent: list online agents for this tenant ─────────────────────────────

    @Transactional(readOnly = true)
    public List<OnlineAgentResponse> listOnlineAgents(UUID tenantId) {
        return userRepository.findChatOnlineAgentsByTenantId(tenantId).stream()
                .map(u -> new OnlineAgentResponse(
                        u.getId(),
                        u.getFullName(),
                        u.getAvatarUrl(),
                        conversationRepository.countActiveByAgent(u.getId())
                ))
                .toList();
    }

    // ── Scheduler: auto-archive CLOSED > 90 days ─────────────────────────────

    @Scheduled(cron = "0 30 2 * * *")
    @Transactional
    public void autoArchiveOld() {
        Instant cutoff = Instant.now().minus(90, ChronoUnit.DAYS);
        List<ChatConversation> old = conversationRepository.findClosedOlderThan(cutoff);
        old.forEach(c -> {
            c.setStatus(ConversationStatus.ARCHIVED);
            c.setArchivedAt(Instant.now());
        });
        conversationRepository.saveAll(old);
        if (!old.isEmpty()) {
            log.info("[Chat] Auto-archived {} conversations older than 90 days", old.size());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public ChatConversation requireConversation(UUID id) {
        return conversationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + id));
    }

    public ChatConversation requireConversationByToken(UUID token) {
        return conversationRepository.findByVisitorToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found for token"));
    }

    private void sendSystemMessage(ChatConversation conv, String text) {
        ChatMessage msg = new ChatMessage();
        msg.setConversation(conv);
        msg.setSenderType(SenderType.SYSTEM);
        msg.setContent(text);
        msg.setRead(true);
        ChatMessage saved = messageRepository.save(msg);

        broadcast(conv.getId(), ChatMessagePayload.builder()
                .type("SYSTEM")
                .id(saved.getId())
                .conversationId(conv.getId())
                .senderType(SenderType.SYSTEM)
                .content(text)
                .createdAt(saved.getCreatedAt())
                .build());
    }

    private void broadcast(UUID conversationId, ChatMessagePayload payload) {
        messagingTemplate.convertAndSend("/topic/chat." + conversationId, payload);
    }

    private ChatMessagePayload buildStatusChangedPayload(UUID convId,
                                                          ConversationStatus status,
                                                          User agent) {
        return ChatMessagePayload.builder()
                .type("STATUS_CHANGED")
                .conversationId(convId)
                .senderName(agent != null ? agent.getFullName() : null)
                .senderAvatarUrl(agent != null ? agent.getAvatarUrl() : null)
                .conversationStatus(status.name())
                .createdAt(Instant.now())
                .build();
    }

    private ChatConversationResponse toResponse(ChatConversation c, User agent, long unread) {
        return ChatConversationResponse.builder()
                .id(c.getId())
                .tenantId(c.getTenantId())
                .visitorId(c.getVisitorId())
                .visitorName(c.getVisitorName())
                .visitorEmail(c.getVisitorEmail())
                .agentId(c.getAgentId())
                .agentName(agent != null ? agent.getFullName() : null)
                .agentAvatarUrl(agent != null ? agent.getAvatarUrl() : null)
                .status(c.getStatus())
                .source(c.getSource())
                .unreadCount(unread)
                .messages(List.of())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .closedAt(c.getClosedAt())
                .build();
    }

    private ChatMessageResponse toMessageResponse(ChatMessage m, User sender) {
        String name = sender != null ? sender.getFullName() : null;
        String avatar = sender != null ? sender.getAvatarUrl() : null;
        return new ChatMessageResponse(
                m.getId(),
                m.getConversation().getId(),
                m.getSenderType(),
                m.getSenderId(),
                name,
                avatar,
                m.getContent(),
                m.getAttachmentUrl(),
                m.isRead(),
                m.getCreatedAt()
        );
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
}
