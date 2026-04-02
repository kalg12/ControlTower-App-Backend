package com.controltower.app.kanban.api;

import com.controltower.app.kanban.api.dto.*;
import com.controltower.app.kanban.application.BoardService;
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

@Tag(name = "Kanban", description = "Kanban boards, columns, cards and checklists")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    // ── Boards ────────────────────────────────────────────────────────

    @Operation(summary = "List boards", description = "Returns a paginated list of Kanban boards accessible to the current tenant. Requires the 'kanban:read' permission.")
    @GetMapping
    @PreAuthorize("hasAuthority('kanban:read')")
    public ResponseEntity<ApiResponse<PageResponse<BoardResponse>>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(boardService.listBoards(pageable))));
    }

    @Operation(summary = "Create board", description = "Creates a new Kanban board owned by the authenticated user. Requires the 'kanban:write' permission.")
    @PostMapping
    @PreAuthorize("hasAuthority('kanban:write')")
    public ResponseEntity<ApiResponse<BoardResponse>> create(
            @Valid @RequestBody BoardRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        UUID userId = UUID.fromString(principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(boardService.createBoard(request, userId)));
    }

    @Operation(summary = "Get board by ID", description = "Retrieves a Kanban board along with its columns and cards. Requires the 'kanban:read' permission.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('kanban:read')")
    public ResponseEntity<ApiResponse<BoardResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(boardService.getBoard(id)));
    }

    @Operation(summary = "Update board", description = "Updates the name and settings of an existing Kanban board. Requires the 'kanban:write' permission.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('kanban:write')")
    public ResponseEntity<ApiResponse<BoardResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody BoardRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(boardService.updateBoard(id, request)));
    }

    @Operation(summary = "Delete board", description = "Permanently deletes a Kanban board and all its columns, cards, and checklist items. Requires the 'kanban:write' permission.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('kanban:write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        boardService.deleteBoard(id);
        return ResponseEntity.ok(ApiResponse.ok("Board deleted"));
    }

    // ── Columns ───────────────────────────────────────────────────────

    @Operation(summary = "Add column to board", description = "Adds a new column to the specified board at the given position. Requires the 'kanban:write' permission.")
    @PostMapping("/{id}/columns")
    @PreAuthorize("hasAuthority('kanban:write')")
    public ResponseEntity<ApiResponse<ColumnResponse>> addColumn(
            @PathVariable UUID id,
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int position) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(boardService.addColumn(id, name, position)));
    }

    @Operation(summary = "Delete column", description = "Removes a column from the board by its ID. Requires the 'kanban:write' permission.")
    @DeleteMapping("/columns/{columnId}")
    @PreAuthorize("hasAuthority('kanban:write')")
    public ResponseEntity<ApiResponse<Void>> deleteColumn(@PathVariable UUID columnId) {
        boardService.deleteColumn(columnId);
        return ResponseEntity.ok(ApiResponse.ok("Column deleted"));
    }

    // ── Cards ─────────────────────────────────────────────────────────

    @Operation(summary = "Create card", description = "Creates a new card in the specified board column. Requires the 'kanban:write' permission.")
    @PostMapping("/cards")
    @PreAuthorize("hasAuthority('kanban:write')")
    public ResponseEntity<ApiResponse<CardResponse>> createCard(@Valid @RequestBody CardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(boardService.createCard(request)));
    }

    @Operation(summary = "Move card", description = "Moves a card to a different column and/or position within the board. Requires the 'kanban:write' permission.")
    @PatchMapping("/cards/{cardId}/move")
    @PreAuthorize("hasAuthority('kanban:write')")
    public ResponseEntity<ApiResponse<CardResponse>> moveCard(
            @PathVariable UUID cardId,
            @Valid @RequestBody MoveCardRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(boardService.moveCard(cardId, request)));
    }

    @Operation(summary = "Delete card", description = "Permanently deletes a card and its checklist items. Requires the 'kanban:write' permission.")
    @DeleteMapping("/cards/{cardId}")
    @PreAuthorize("hasAuthority('kanban:write')")
    public ResponseEntity<ApiResponse<Void>> deleteCard(@PathVariable UUID cardId) {
        boardService.deleteCard(cardId);
        return ResponseEntity.ok(ApiResponse.ok("Card deleted"));
    }

    // ── Checklist ─────────────────────────────────────────────────────

    @Operation(summary = "Add checklist item", description = "Appends a new checklist item to the specified card. Requires the 'kanban:write' permission.")
    @PostMapping("/cards/{cardId}/checklist")
    @PreAuthorize("hasAuthority('kanban:write')")
    public ResponseEntity<ApiResponse<ChecklistItemResponse>> addChecklistItem(
            @PathVariable UUID cardId,
            @RequestParam String text) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(boardService.addChecklistItem(cardId, text)));
    }

    @Operation(summary = "Toggle checklist item", description = "Toggles the completion state of a checklist item between checked and unchecked. Requires the 'kanban:write' permission.")
    @PatchMapping("/checklist/{itemId}/toggle")
    @PreAuthorize("hasAuthority('kanban:write')")
    public ResponseEntity<ApiResponse<ChecklistItemResponse>> toggle(@PathVariable UUID itemId) {
        return ResponseEntity.ok(ApiResponse.ok(boardService.toggleChecklistItem(itemId)));
    }
}
