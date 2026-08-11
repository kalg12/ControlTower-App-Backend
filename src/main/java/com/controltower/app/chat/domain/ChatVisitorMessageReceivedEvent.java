package com.controltower.app.chat.domain;

import com.controltower.app.shared.events.DomainEvent;
import lombok.Getter;

import java.util.UUID;

@Getter
public class ChatVisitorMessageReceivedEvent extends DomainEvent {

    private final UUID conversationId;
    private final UUID tenantId;
    private final UUID assignedAgentId;
    private final String visitorName;
    private final String content;

    public ChatVisitorMessageReceivedEvent(ChatConversation conversation, String content) {
        this.conversationId = conversation.getId();
        this.tenantId = conversation.getTenantId();
        this.assignedAgentId = conversation.getAgentId();
        this.visitorName = conversation.getVisitorName();
        this.content = content;
    }

    @Override
    public String getEventType() {
        return "chat.visitor.message.received";
    }
}
