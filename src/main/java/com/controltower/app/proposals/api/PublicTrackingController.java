package com.controltower.app.proposals.api;

import com.controltower.app.proposals.application.ProposalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/tracking")
@RequiredArgsConstructor
public class PublicTrackingController {

    // 1×1 transparent GIF (35 bytes)
    private static final byte[] PIXEL = Base64.getDecoder().decode(
            "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");

    private final ProposalService proposalService;

    @GetMapping("/{token}/pixel.gif")
    public ResponseEntity<byte[]> trackOpen(@PathVariable UUID token) {
        proposalService.markEmailViewed(token);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/gif"))
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(PIXEL);
    }
}
