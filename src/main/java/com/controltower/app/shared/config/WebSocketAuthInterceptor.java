package com.controltower.app.shared.config;

import com.controltower.app.chat.domain.ChatConversation;
import com.controltower.app.chat.domain.ChatConversationRepository;
import com.controltower.app.identity.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Validates STOMP CONNECT frames. Accepts two auth modes:
 *  - Agents: JWT in "Authorization: Bearer <token>" header
 *  - Visitors (POS widget): UUID in "X-Visitor-Token" header
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final ChatConversationRepository chatConversationRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            // ── Visitor auth (POS widget) ────────────────────────────────────
            String visitorTokenHeader = accessor.getFirstNativeHeader("X-Visitor-Token");
            if (visitorTokenHeader != null) {
                try {
                    UUID visitorToken = UUID.fromString(visitorTokenHeader);
                    Optional<ChatConversation> conv = chatConversationRepository.findByVisitorToken(visitorToken);
                    if (conv.isEmpty()) {
                        log.warn("WebSocket CONNECT rejected: invalid visitor token");
                        throw new org.springframework.security.access.AccessDeniedException("Invalid visitor token");
                    }
                    String principal = "visitor:" + conv.get().getId();
                    accessor.setUser(new UsernamePasswordAuthenticationToken(
                            principal, null, List.of(new SimpleGrantedAuthority("ROLE_VISITOR"))));
                    log.debug("WebSocket CONNECT accepted for visitor, conversationId={}", conv.get().getId());
                } catch (IllegalArgumentException e) {
                    throw new org.springframework.security.access.AccessDeniedException("Malformed visitor token");
                }
                return message;
            }

            // ── Agent auth (JWT) ─────────────────────────────────────────────
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("WebSocket CONNECT rejected: missing Authorization header");
                throw new org.springframework.security.access.AccessDeniedException(
                        "WebSocket connection requires a valid Bearer token or visitor token");
            }

            String token = authHeader.substring(7);
            if (!jwtTokenProvider.validateToken(token) || jwtTokenProvider.isMfaPending(token)) {
                log.warn("WebSocket CONNECT rejected: invalid or incomplete token");
                throw new org.springframework.security.access.AccessDeniedException(
                        "Invalid or expired JWT token");
            }

            String userId = jwtTokenProvider.getUserId(token).toString();
            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
            log.debug("WebSocket CONNECT accepted for userId={}", userId);
        }

        return message;
    }
}
