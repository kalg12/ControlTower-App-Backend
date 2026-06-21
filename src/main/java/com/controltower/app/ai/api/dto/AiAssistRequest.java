package com.controltower.app.ai.api.dto;

import jakarta.validation.constraints.NotNull;

public record AiAssistRequest(
    @NotNull AiTask task,
    AiContext context
) {
    public enum AiTask {
        GENERATE_CARD_PROMPT,
        IMPROVE_TICKET_REPLY,
        QUICK_REPLY
    }

    public record AiContext(
        // Kanban card context
        String cardTitle,
        String cardDescription,
        java.util.List<String> cardChecklist,
        String cardPriority,
        String boardName,
        String clientName,

        // Ticket context
        String ticketSubject,
        String ticketDescription,
        String draftReply,
        QuickReplyType quickReplyType
    ) {}

    public enum QuickReplyType {
        STARTED_REVIEW,
        WAITING_CLIENT,
        CLOSE_TICKET,
        NEED_INFO,
        SCHEDULE_CALL
    }
}
