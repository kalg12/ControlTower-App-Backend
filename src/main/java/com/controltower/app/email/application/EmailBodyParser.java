package com.controltower.app.email.application;

import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;

/** Extracts plain text and HTML body parts from a MIME message. */
public final class EmailBodyParser {

    private EmailBodyParser() {}

    public record Result(String text, String html) {}

    public static Result parse(MimeMessage message) throws MessagingException, IOException {
        StringBuilder text = new StringBuilder();
        StringBuilder html = new StringBuilder();
        collectParts(message, text, html);
        return new Result(
            text.isEmpty() ? null : text.toString().trim(),
            html.isEmpty() ? null : html.toString().trim()
        );
    }

    private static void collectParts(Part part, StringBuilder text, StringBuilder html)
            throws MessagingException, IOException {
        String contentType = part.getContentType().toLowerCase();

        if (part.isMimeType("text/plain")) {
            text.append(part.getContent());
        } else if (part.isMimeType("text/html")) {
            html.append(part.getContent());
        } else if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                // Skip attachments (has filename or is not inline)
                String disposition = bodyPart.getDisposition();
                if (Part.ATTACHMENT.equalsIgnoreCase(disposition)) continue;
                collectParts(bodyPart, text, html);
            }
        }
        // Other content types (images, attachments) are ignored here
    }
}
