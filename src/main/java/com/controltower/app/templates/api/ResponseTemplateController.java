package com.controltower.app.templates.api;

import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.shared.response.PageResponse;
import com.controltower.app.templates.api.dto.ResponseTemplateRequest;
import com.controltower.app.templates.application.ResponseTemplateService;
import com.controltower.app.templates.domain.ResponseTemplate;
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

@Tag(name = "Response Templates", description = "Canned responses / macros for support agents")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class ResponseTemplateController {

    private final ResponseTemplateService service;

    @GetMapping
    @PreAuthorize("hasAuthority('template:read')")
    @Operation(summary = "List templates", description = "Returns paginated templates, optionally filtered by category or full-text ?q=")
    public ResponseEntity<ApiResponse<PageResponse<ResponseTemplate>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(service.list(q, category, pageable))));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('template:write')")
    @Operation(summary = "Create template")
    public ResponseEntity<ApiResponse<ResponseTemplate>> create(
            @Valid @RequestBody ResponseTemplateRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        UUID authorId = UUID.fromString(principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(service.create(req, authorId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('template:write')")
    @Operation(summary = "Update template")
    public ResponseEntity<ApiResponse<ResponseTemplate>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ResponseTemplateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('template:write')")
    @Operation(summary = "Delete template")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.<Void>ok("Template deleted", null));
    }
}
