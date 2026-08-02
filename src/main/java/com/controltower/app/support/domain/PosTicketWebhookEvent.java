package com.controltower.app.support.domain;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/** Outbound POS webhook, dispatched only after the owning transaction commits. */
@Getter
public class PosTicketWebhookEvent {

    public enum Type { STATUS_CHANGE, NEW_COMMENT }

    private final Type type;
    private final String posTicketId;
    private final String callbackUrl;
    private final String ctStatus;
    private final UUID commentId;
    private final String content;
    private final String senderName;
    private final Instant occurredAt;

    private PosTicketWebhookEvent(Type type, String posTicketId, String callbackUrl,
                                  String ctStatus, UUID commentId, String content,
                                  String senderName, Instant occurredAt) {
        this.type = type;
        this.posTicketId = posTicketId;
        this.callbackUrl = callbackUrl;
        this.ctStatus = ctStatus;
        this.commentId = commentId;
        this.content = content;
        this.senderName = senderName;
        this.occurredAt = occurredAt != null ? occurredAt : Instant.now();
    }

    public static PosTicketWebhookEvent statusChange(String posTicketId, String callbackUrl,
                                                      String ctStatus) {
        return new PosTicketWebhookEvent(Type.STATUS_CHANGE, posTicketId, callbackUrl,
                ctStatus, null, null, null, Instant.now());
    }

    public static PosTicketWebhookEvent newComment(String posTicketId, String callbackUrl,
                                                   UUID commentId, String content,
                                                   String senderName, Instant occurredAt) {
        return new PosTicketWebhookEvent(Type.NEW_COMMENT, posTicketId, callbackUrl,
                null, commentId, content, senderName, occurredAt);
    }
}
