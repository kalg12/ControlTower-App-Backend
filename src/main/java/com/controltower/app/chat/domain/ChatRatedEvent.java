package com.controltower.app.chat.domain;

import com.controltower.app.shared.events.DomainEvent;
import lombok.Getter;

import java.util.UUID;

@Getter
public class ChatRatedEvent extends DomainEvent {

    private final UUID conversationId;
    private final UUID tenantId;
    private final UUID assignedAgentId;
    private final String visitorName;
    private final int rating;
    private final String comment;

    public ChatRatedEvent(ChatConversation conversation, int rating, String comment) {
        this.conversationId = conversation.getId();
        this.tenantId = conversation.getTenantId();
        this.assignedAgentId = conversation.getAgentId();
        this.visitorName = conversation.getVisitorName();
        this.rating = rating;
        this.comment = comment;
    }

    @Override
    public String getEventType() {
        return "chat.conversation.rated";
    }
}
