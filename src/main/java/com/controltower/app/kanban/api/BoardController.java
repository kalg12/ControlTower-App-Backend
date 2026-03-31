package com.controltower.app.kanban.api;

import com.controltower.app.kanban.api.dto.BoardRequest;
import com.controltower.app.kanban.api.dto.CardRequest;
import com.controltower.app.kanban.api.dto.MoveCardRequest;
import com.controltower.app.kanban.application.BoardService;
import com.controltower.app.kanban.domain.Board;
import com.controltower.app.kanban.domain.Card;
import com.controltower.app.kanban.domain.ChecklistItem;
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

    @GetMapping
    @PreAuthorize("hasAuthority('kanban:read')")
    public ResponseEntity<ApiResponse<PageResponse<Board>>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(boardService.listBoards(pageable))));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('kanban:write')")
    public ResponseEntity<ApiResponse<Board>> create(
            @Valid @RequestBody BoardRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        UUID userId = UUID.fromString(principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(boardService.createBoard(request, userId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('kanban:read')")
    public ResponseEntity<ApiResponse<Board>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(boardService.getBoard(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('kanban:write')")
    public ResponseEntity<ApiResponse<Board>> update(
            @PathVariable UUID id,
            @Valid @RequestBody BoardRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(boardService.updateBoard(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('kanban:write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        boardService.deleteBoard(id);
        return ResponseEntity.ok(ApiResponse.ok("Board deleted"));
    }

    // ── Columns ───────────────────────────────────────────────────────

    @PostMapping("/{id}/columns")
    @PreAuthorize("hasAuthority('kanban:write')")
    public ResponseEntity<ApiResponse<Void>> addColumn(
            @PathVariable UUID id,
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int position) {
        boardService.addColumn(id, name, position);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Column added"));
    }

    @DeleteMapping("/columns/{columnId}")
    @PreAuthorize("hasAuthority('kanban:write')")
    public ResponseEntity<ApiResponse<Void>> deleteColumn(@PathVariable UUID columnId) {
        boardService.deleteColumn(columnId);
        return ResponseEntity.ok(ApiResponse.ok("Column deleted"));
    }

    // ── Cards ─────────────────────────────────────────────────────────

    @PostMapping("/cards")
    @PreAuthorize("hasAuthority('kanban:write')")
    public ResponseEntity<ApiResponse<Card>> createCard(@Valid @RequestBody CardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(boardService.createCard(request)));
    }

    @PatchMapping("/cards/{cardId}/move")
    @PreAuthorize("hasAuthority('kanban:write')")
    public ResponseEntity<ApiResponse<Card>> moveCard(
            @PathVariable UUID cardId,
            @Valid @RequestBody MoveCardRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(boardService.moveCard(cardId, request)));
    }

    @DeleteMapping("/cards/{cardId}")
    @PreAuthorize("hasAuthority('kanban:write')")
    public ResponseEntity<ApiResponse<Void>> deleteCard(@PathVariable UUID cardId) {
        boardService.deleteCard(cardId);
        return ResponseEntity.ok(ApiResponse.ok("Card deleted"));
    }

    // ── Checklist ─────────────────────────────────────────────────────

    @PostMapping("/cards/{cardId}/checklist")
    @PreAuthorize("hasAuthority('kanban:write')")
    public ResponseEntity<ApiResponse<ChecklistItem>> addChecklistItem(
            @PathVariable UUID cardId,
            @RequestParam String text) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(boardService.addChecklistItem(cardId, text)));
    }

    @PatchMapping("/checklist/{itemId}/toggle")
    @PreAuthorize("hasAuthority('kanban:write')")
    public ResponseEntity<ApiResponse<ChecklistItem>> toggle(@PathVariable UUID itemId) {
        return ResponseEntity.ok(ApiResponse.ok(boardService.toggleChecklistItem(itemId)));
    }
}
