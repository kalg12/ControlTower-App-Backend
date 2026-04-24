package com.controltower.app.chat.api;

import com.controltower.app.chat.api.dto.ChatMessagePayload;
import com.controltower.app.chat.api.dto.SendMessageRequest;
import com.controltower.app.chat.application.ChatService;
import com.controltower.app.chat.domain.ChatConversation;
import com.controltower.app.chat.domain.SenderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /** Visitor sends a message */
    @MessageMapping("/chat.visitor.message")
    public void visitorMessage(@Payload SendMessageRequest req, Principal principal) {
        UUID conversationId = extractConversationId(principal);
        if (conversationId == null) return;
        chatService.sendMessage(conversationId, SenderType.VISITOR, null, req.content());
    }

    /** Agent sends a message */
    @MessageMapping("/chat.agent.message")
    public void agentMessage(@Payload SendMessageRequest req, Principal principal) {
        UUID agentId = extractAgentId(principal);
        if (agentId == null || req.conversationId() == null) return;
        chatService.sendMessage(req.conversationId(), SenderType.AGENT, agentId, req.content());
    }

    /** Typing indicator — ephemeral, not persisted */
    @MessageMapping("/chat.typing")
    public void typingIndicator(@Payload SendMessageRequest req, Principal principal) {
        UUID conversationId = req.conversationId();
        if (conversationId == null) {
            conversationId = extractConversationId(principal);
        }
        if (conversationId == null) return;

        boolean isAgent = principal.getName() != null && !principal.getName().startsWith("visitor:");
        ChatMessagePayload payload = ChatMessagePayload.builder()
                .type("TYPING")
                .conversationId(conversationId)
                .senderType(isAgent ? SenderType.AGENT : SenderType.VISITOR)
                .createdAt(Instant.now())
                .build();

        messagingTemplate.convertAndSend("/topic/chat." + conversationId, payload);
    }

    /** Visitor joins — creates a conversation via WebSocket (alternative to REST) */
    @MessageMapping("/chat.visitor.join")
    public void visitorJoin(@Payload SendMessageRequest req, Principal principal) {
        UUID conversationId = extractConversationId(principal);
        if (conversationId == null) return;

        ChatConversation conv = chatService.requireConversation(conversationId);
        ChatMessagePayload joinPayload = ChatMessagePayload.builder()
                .type("JOINED")
                .conversationId(conversationId)
                .senderType(SenderType.VISITOR)
                .senderName(conv.getVisitorName())
                .content("Visitante conectado")
                .createdAt(Instant.now())
                .build();

        messagingTemplate.convertAndSend("/topic/chat." + conversationId, joinPayload);
        messagingTemplate.convertAndSend("/topic/chat.queue." + conv.getTenantId(), joinPayload);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private UUID extractConversationId(Principal principal) {
        if (principal == null) return null;
        String name = principal.getName();
        if (name != null && name.startsWith("visitor:")) {
            try {
                return UUID.fromString(name.substring("visitor:".length()));
            } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }

    private UUID extractAgentId(Principal principal) {
        if (principal == null) return null;
        String name = principal.getName();
        if (name != null && !name.startsWith("visitor:")) {
            try {
                return UUID.fromString(name);
            } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }
}
