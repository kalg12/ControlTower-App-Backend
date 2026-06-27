package com.controltower.app.chat.domain;

import com.controltower.app.shared.events.DomainEvent;
import lombok.Getter;

import java.util.UUID;

@Getter
public class ChatConversationStartedEvent extends DomainEvent {

    private final UUID conversationId;
    private final UUID tenantId;
    private final String visitorName;
    private final String visitorEmail;
    private final String source;

    public ChatConversationStartedEvent(ChatConversation conversation) {
        this.conversationId = conversation.getId();
        this.tenantId       = conversation.getTenantId();
        this.visitorName    = conversation.getVisitorName();
        this.visitorEmail   = conversation.getVisitorEmail();
        this.source         = conversation.getSource();
    }

    @Override
    public String getEventType() {
        return "chat.conversation.started";
    }
}
