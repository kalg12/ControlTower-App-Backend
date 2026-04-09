package com.controltower.app.support.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * Published when a POS user sends a chat message via POS_CHAT_MESSAGE integration event.
 * Triggers a WebSocket notification to CT operators so they know there is a new message.
 */
@Getter
@RequiredArgsConstructor
public class PosTicketChatEvent {
    private final UUID tenantId;
    private final UUID ticketId;
    private final String posTicketId;
    private final String senderName;
    private final String content;
    private final String branchName;
}
