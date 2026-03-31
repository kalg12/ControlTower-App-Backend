package com.controltower.app.notes.api;

import com.controltower.app.notes.api.dto.NoteRequest;
import com.controltower.app.notes.application.NoteService;
import com.controltower.app.notes.domain.Note;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.shared.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Tag(name = "Notes", description = "Rich-text notes linked to clients, tickets or branches")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @Operation(summary = "List notes", description = "Returns a paginated list of notes, optionally filtered by linked entity type and ID. Requires the user to be authenticated.")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<Note>>> list(
            @RequestParam(required = false) String linkedTo,
            @RequestParam(required = false) UUID linkedId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(
                noteService.listNotes(linkedTo, linkedId, pageable))));
    }

    @Operation(summary = "Create note", description = "Creates a new note authored by the authenticated user, optionally linked to a client, ticket, or branch. Requires authentication.")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Note>> create(
            @Valid @RequestBody NoteRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        UUID authorId = UUID.fromString(principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(noteService.createNote(request, authorId)));
    }

    @Operation(summary = "Get note by ID", description = "Retrieves a single note by its UUID. Requires authentication.")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Note>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(noteService.getNote(id)));
    }

    @Operation(summary = "Update note", description = "Replaces the content of an existing note with the provided data. Requires authentication.")
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Note>> update(
            @PathVariable UUID id,
            @Valid @RequestBody NoteRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(noteService.updateNote(id, request)));
    }

    @Operation(summary = "Delete note", description = "Permanently removes a note by its UUID. Requires authentication.")
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        noteService.deleteNote(id);
        return ResponseEntity.ok(ApiResponse.ok("Note deleted"));
    }
}
