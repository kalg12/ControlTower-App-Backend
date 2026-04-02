package com.controltower.app.campaigns.api;

import com.controltower.app.campaigns.api.dto.CampaignRequest;
import com.controltower.app.campaigns.api.dto.CampaignResponse;
import com.controltower.app.campaigns.api.dto.CampaignUpdateRequest;
import com.controltower.app.campaigns.application.CampaignService;
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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Campaigns", description = "Marketing campaign management")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    @Operation(summary = "List campaigns")
    @GetMapping
    @PreAuthorize("hasAuthority('campaign:read')")
    public ResponseEntity<ApiResponse<PageResponse<CampaignResponse>>> list(
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "20")  int size,
            @RequestParam(required = false)     String search) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(campaignService.list(search, pageable))));
    }

    @Operation(summary = "Create campaign")
    @PostMapping
    @PreAuthorize("hasAuthority('campaign:write')")
    public ResponseEntity<ApiResponse<CampaignResponse>> create(@Valid @RequestBody CampaignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(campaignService.create(request)));
    }

    @Operation(summary = "Update campaign")
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('campaign:write')")
    public ResponseEntity<ApiResponse<CampaignResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CampaignUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(campaignService.update(id, request)));
    }

    @Operation(summary = "Send campaign")
    @PostMapping("/{id}/send")
    @PreAuthorize("hasAuthority('campaign:write')")
    public ResponseEntity<ApiResponse<Void>> send(@PathVariable UUID id) {
        campaignService.send(id);
        return ResponseEntity.ok(ApiResponse.ok("Campaign sent"));
    }

    @Operation(summary = "Delete campaign")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('campaign:write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        campaignService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Campaign deleted"));
    }
}
