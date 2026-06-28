package com.controltower.app.notes.api.dto;

import com.controltower.app.notes.domain.Note;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NoteResponse(
        UUID id,
        UUID authorId,
        String authorName,
        String title,
        String content,
        String linkedTo,
        UUID linkedId,
        UUID parentId,
        List<NoteResponse> replies,
        Instant createdAt,
        Instant updatedAt
) {
    public static NoteResponse from(Note note, String authorName, List<NoteResponse> replies) {
        return new NoteResponse(
                note.getId(),
                note.getAuthorId(),
                authorName,
                note.getTitle(),
                note.getContent(),
                note.getLinkedTo(),
                note.getLinkedId(),
                note.getParentId(),
                replies != null ? replies : List.of(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
