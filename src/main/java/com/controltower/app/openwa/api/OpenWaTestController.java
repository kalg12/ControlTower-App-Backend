package com.controltower.app.openwa.api;

import com.controltower.app.shared.infrastructure.OpenWaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/internal/openwa")
@RequiredArgsConstructor
@Tag(name = "openwa-internal", description = "Internal OpenWA test endpoints")
public class OpenWaTestController {

    private final OpenWaService openWaService;

    @PostMapping("/test")
    @PreAuthorize("hasAuthority('health:read')")
    @Operation(summary = "Send a test WhatsApp alert to the dev group")
    public ResponseEntity<Map<String, Object>> sendTest(
            @RequestBody(required = false) Map<String, String> body) {

        String message = (body != null && body.containsKey("message") && !body.get("message").isBlank())
                ? body.get("message")
                : "🧪 TEST Control Tower — Alerta de prueba\n\nHora UTC: " + Instant.now() + "\n\nConexión con OpenWA operativa.";

        openWaService.sendDevAlert(message);

        return ResponseEntity.ok(Map.of(
                "queued", true,
                "message", message
        ));
    }
}
