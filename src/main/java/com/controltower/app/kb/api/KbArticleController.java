package com.controltower.app.kb.api;

import com.controltower.app.kb.api.dto.KbArticleRequest;
import com.controltower.app.kb.api.dto.KbArticleResponse;
import com.controltower.app.kb.application.KbArticleService;
import com.controltower.app.kb.domain.KbStatus;
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

@Tag(name = "Knowledge Base", description = "Internal knowledge base articles for support agents")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/kb/articles")
@RequiredArgsConstructor
public class KbArticleController {

    private final KbArticleService kbArticleService;

    @GetMapping
    @PreAuthorize("hasAuthority('kb:read')")
    @Operation(summary = "List or search articles", description = "Returns paginated articles. If ?q= is provided, performs a full-text search on title and content.")
    public ResponseEntity<ApiResponse<PageResponse<KbArticleResponse>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) KbStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(
                kbArticleService.list(q, status, category, pageable))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('kb:read')")
    @Operation(summary = "Get article by ID", description = "Returns article details and increments view count.")
    public ResponseEntity<ApiResponse<KbArticleResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(kbArticleService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('kb:write')")
    @Operation(summary = "Create article")
    public ResponseEntity<ApiResponse<KbArticleResponse>> create(
            @Valid @RequestBody KbArticleRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        UUID authorId = UUID.fromString(principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(kbArticleService.create(request, authorId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('kb:write')")
    @Operation(summary = "Update article")
    public ResponseEntity<ApiResponse<KbArticleResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody KbArticleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(kbArticleService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('kb:write')")
    @Operation(summary = "Delete article")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        kbArticleService.delete(id);
        return ResponseEntity.ok(ApiResponse.<Void>ok("Article deleted"));
    }
}
