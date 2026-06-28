package com.controltower.app.notes.application;

import com.controltower.app.identity.domain.User;
import com.controltower.app.identity.domain.UserRepository;
import com.controltower.app.notes.api.dto.NoteRequest;
import com.controltower.app.notes.api.dto.NoteResponse;
import com.controltower.app.notes.domain.Note;
import com.controltower.app.notes.domain.NoteRepository;
import com.controltower.app.notifications.application.NotificationService;
import com.controltower.app.notifications.domain.Notification;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.shared.infrastructure.EmailService;
import com.controltower.app.support.domain.TicketRepository;
import com.controltower.app.kanban.domain.CardRepository;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository     noteRepository;
    private final UserRepository     userRepository;
    private final NotificationService notificationService;
    private final EmailService       emailService;
    private final TicketRepository   ticketRepository;
    private final CardRepository     cardRepository;

    // ── Read ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<NoteResponse> listNotes(String linkedTo, UUID linkedId, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        Page<Note> page;
        if (linkedTo != null && linkedId != null) {
            page = noteRepository.findByTenantIdAndLinkedToAndLinkedIdAndParentIdIsNullAndDeletedAtIsNull(
                    tenantId, linkedTo, linkedId, pageable);
        } else {
            page = noteRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable);
        }
        return page.map(note -> toResponse(note, true));
    }

    @Transactional(readOnly = true)
    public NoteResponse getNote(UUID noteId) {
        return toResponse(resolve(noteId), true);
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    @Transactional
    public NoteResponse createNote(NoteRequest request, UUID authorId) {
        Note note = new Note();
        note.setTenantId(TenantContext.getTenantId());
        note.setAuthorId(authorId);
        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setLinkedTo(request.getLinkedTo());
        note.setLinkedId(request.getLinkedId());
        note.setParentId(request.getParentId());
        Note saved = noteRepository.save(note);

        String authorName = resolveAuthorName(authorId);
        fireNoteNotifications(saved, authorName, false);
        log.info("[Notes] Note {} created by {} on {}/{}", saved.getId(), authorName,
                saved.getLinkedTo(), saved.getLinkedId());

        return toResponse(saved, false);
    }

    @Transactional
    public NoteResponse createReply(UUID parentNoteId, NoteRequest request, UUID authorId) {
        Note parent = resolve(parentNoteId);

        Note reply = new Note();
        reply.setTenantId(TenantContext.getTenantId());
        reply.setAuthorId(authorId);
        // Replies inherit subject from the parent note (title is the reply content preview)
        reply.setTitle(request.getTitle() != null ? request.getTitle() : parent.getTitle());
        reply.setContent(request.getContent());
        reply.setLinkedTo(parent.getLinkedTo());
        reply.setLinkedId(parent.getLinkedId());
        reply.setParentId(parentNoteId);
        Note saved = noteRepository.save(reply);

        String authorName = resolveAuthorName(authorId);
        fireNoteNotifications(saved, authorName, true);
        log.info("[Notes] Reply {} to note {} created by {}", saved.getId(), parentNoteId, authorName);

        return toResponse(saved, false);
    }

    @Transactional
    public NoteResponse updateNote(UUID noteId, NoteRequest request) {
        Note note = resolve(noteId);
        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        return toResponse(noteRepository.save(note), true);
    }

    @Transactional
    public void deleteNote(UUID noteId) {
        Note note = resolve(noteId);
        note.softDelete();
        noteRepository.save(note);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private NoteResponse toResponse(Note note, boolean includeReplies) {
        String authorName = resolveAuthorName(note.getAuthorId());
        List<NoteResponse> replies = List.of();
        if (includeReplies && note.getParentId() == null) {
            replies = noteRepository.findByParentIdAndDeletedAtIsNullOrderByCreatedAtAsc(note.getId())
                    .stream()
                    .map(r -> toResponse(r, false))
                    .collect(Collectors.toList());
        }
        return NoteResponse.from(note, authorName, replies);
    }

    private String resolveAuthorName(UUID authorId) {
        if (authorId == null) return "Sistema";
        return userRepository.findById(authorId)
                .map(User::getFullName)
                .orElse("Usuario");
    }

    private Note resolve(UUID noteId) {
        return noteRepository.findByIdAndTenantIdAndDeletedAtIsNull(noteId, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Note", noteId));
    }

    /** Fire in-app notification + email to recipients relevant to the linked entity. */
    private void fireNoteNotifications(Note note, String authorName, boolean isReply) {
        UUID tenantId = note.getTenantId();
        String type = isReply ? "NOTE_REPLY_ADDED" : "NOTE_ADDED";
        String title = isReply
                ? "Nueva respuesta en nota"
                : "Nueva nota interna";
        String body = authorName + (isReply ? " respondió una nota" : " escribió una nota")
                + (note.getTitle() != null ? ": " + truncate(note.getTitle(), 80) : "");

        List<UUID> recipients = resolveRecipients(note);
        if (recipients.isEmpty()) return;

        try {
            notificationService.send(
                    tenantId, type, title, body,
                    Notification.Severity.INFO,
                    Map.<String, Object>of(
                            "noteId",    note.getId().toString(),
                            "linkedTo",  note.getLinkedTo() != null ? note.getLinkedTo() : "",
                            "linkedId",  note.getLinkedId() != null ? note.getLinkedId().toString() : "",
                            "isReply",   String.valueOf(isReply)
                    ),
                    recipients
            );
        } catch (Exception e) {
            log.warn("[Notes] Could not send in-app notification for note {}: {}", note.getId(), e.getMessage());
        }

        // Email notification
        recipients.forEach(recipientId ->
                userRepository.findById(recipientId).ifPresent(u -> {
                    try {
                        emailService.sendNoteNotification(
                                u.getEmail(), u.getFullName(),
                                authorName, note.getTitle(), note.getContent(),
                                note.getLinkedTo(), isReply);
                    } catch (Exception e) {
                        log.warn("[Notes] Could not send email notification to {}: {}", u.getEmail(), e.getMessage());
                    }
                })
        );
    }

    private List<UUID> resolveRecipients(Note note) {
        if (note.getLinkedTo() == null || note.getLinkedId() == null) return List.of();
        UUID authorId = note.getAuthorId();
        UUID linkedId  = note.getLinkedId();

        if ("TICKET".equals(note.getLinkedTo())) {
            try {
                return ticketRepository.findById(linkedId)
                        .map(t -> {
                            List<UUID> ids = new ArrayList<>();
                            if (t.getAssigneeId() != null) ids.add(t.getAssigneeId());
                            return ids.stream()
                                    .filter(id -> !id.equals(authorId))
                                    .distinct()
                                    .collect(Collectors.toList());
                        })
                        .orElse(List.of());
            } catch (Exception e) {
                log.warn("[Notes] Could not resolve TICKET recipients: {}", e.getMessage());
                return List.of();
            }
        }

        if ("KANBAN_CARD".equals(note.getLinkedTo())) {
            try {
                return cardRepository.findById(linkedId)
                        .map(c -> c.getAssigneeIds().stream()
                                .filter(id -> !id.equals(authorId))
                                .collect(Collectors.toList()))
                        .orElse(List.of());
            } catch (Exception e) {
                log.warn("[Notes] Could not resolve KANBAN_CARD recipients: {}", e.getMessage());
                return List.of();
            }
        }

        return List.of();
    }

    private static String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
