package com.controltower.app.notes.application;

import com.controltower.app.notes.api.dto.NoteRequest;
import com.controltower.app.notes.domain.Note;
import com.controltower.app.notes.domain.NoteRepository;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;

    @Transactional(readOnly = true)
    public Page<Note> listNotes(String linkedTo, UUID linkedId, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        if (linkedTo != null && linkedId != null) {
            return noteRepository.findByTenantIdAndLinkedToAndLinkedIdAndDeletedAtIsNull(
                    tenantId, linkedTo, linkedId, pageable);
        }
        return noteRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable);
    }

    @Transactional
    public Note createNote(NoteRequest request, UUID authorId) {
        Note note = new Note();
        note.setTenantId(TenantContext.getTenantId());
        note.setAuthorId(authorId);
        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setLinkedTo(request.getLinkedTo());
        note.setLinkedId(request.getLinkedId());
        return noteRepository.save(note);
    }

    @Transactional(readOnly = true)
    public Note getNote(UUID noteId) {
        return resolve(noteId);
    }

    @Transactional
    public Note updateNote(UUID noteId, NoteRequest request) {
        Note note = resolve(noteId);
        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        return noteRepository.save(note);
    }

    @Transactional
    public void deleteNote(UUID noteId) {
        Note note = resolve(noteId);
        note.softDelete();
        noteRepository.save(note);
    }

    private Note resolve(UUID noteId) {
        return noteRepository.findByIdAndTenantIdAndDeletedAtIsNull(noteId, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Note", noteId));
    }
}
