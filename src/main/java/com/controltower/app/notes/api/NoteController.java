package com.controltower.app.notes.api;

import com.controltower.app.notes.api.dto.NoteRequest;
import com.controltower.app.notes.api.dto.NoteResponse;
import com.controltower.app.notes.application.NoteService;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Notes", description = "Internal notes linked to tickets, kanban cards or clients")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List notes (root only, replies embedded)")
    public ResponseEntity<ApiResponse<PageResponse<NoteResponse>>> list(
            @RequestParam(required = false) String linkedTo,
            @RequestParam(required = false) UUID linkedId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(
                noteService.listNotes(linkedTo, linkedId, pageable))));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a note")
    public ResponseEntity<ApiResponse<NoteResponse>> create(
            @Valid @RequestBody NoteRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        UUID authorId = UUID.fromString(principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(noteService.createNote(request, authorId)));
    }

    @PostMapping("/{id}/replies")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Reply to a note")
    public ResponseEntity<ApiResponse<NoteResponse>> createReply(
            @PathVariable UUID id,
            @Valid @RequestBody NoteRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        UUID authorId = UUID.fromString(principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(noteService.createReply(id, request, authorId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get note by ID")
    public ResponseEntity<ApiResponse<NoteResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(noteService.getNote(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update note")
    public ResponseEntity<ApiResponse<NoteResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody NoteRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(noteService.updateNote(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete note")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        noteService.deleteNote(id);
        return ResponseEntity.ok(ApiResponse.ok("Note deleted"));
    }
}
