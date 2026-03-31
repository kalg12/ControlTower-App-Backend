package com.controltower.app.shared.config;

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

/**
 * Validates the JWT token on STOMP CONNECT frames.
 * Clients must send the token in the STOMP "Authorization" header:
 *   Authorization: Bearer <access_token>
 *
 * If the token is missing or invalid the CONNECT is rejected with a security exception.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("WebSocket CONNECT rejected: missing Authorization header");
                throw new org.springframework.security.access.AccessDeniedException(
                        "WebSocket connection requires a valid Bearer token");
            }

            String token = authHeader.substring(7);
            if (!jwtTokenProvider.validateToken(token) || jwtTokenProvider.isMfaPending(token)) {
                log.warn("WebSocket CONNECT rejected: invalid or incomplete token");
                throw new org.springframework.security.access.AccessDeniedException(
                        "Invalid or expired JWT token");
            }

            String userId = jwtTokenProvider.getUserId(token).toString();
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

            accessor.setUser(auth);
            log.debug("WebSocket CONNECT accepted for userId={}", userId);
        }

        return message;
    }
}
