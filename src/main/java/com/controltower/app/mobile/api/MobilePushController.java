package com.controltower.app.mobile.api;

import com.controltower.app.mobile.domain.MobilePushToken;
import com.controltower.app.mobile.domain.MobilePushTokenRepository;
import com.controltower.app.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Tag(name = "Mobile", description = "Mobile app push token management")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/mobile")
@RequiredArgsConstructor
public class MobilePushController {

    private final MobilePushTokenRepository tokenRepo;

    public record RegisterTokenRequest(
        @NotBlank String token,
        @NotBlank String platform,
        Map<String, Object> deviceInfo
    ) {}

    @PostMapping("/push-tokens")
    @Operation(summary = "Register or refresh Expo push token for current user")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody RegisterTokenRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());

        tokenRepo.findByUserIdAndToken(userId, req.token()).ifPresentOrElse(
            existing -> {
                existing.setActive(true);
                existing.setPlatform(req.platform());
                if (req.deviceInfo() != null) existing.setDeviceInfo(req.deviceInfo());
                tokenRepo.save(existing);
            },
            () -> {
                MobilePushToken token = new MobilePushToken();
                token.setUserId(userId);
                token.setToken(req.token());
                token.setPlatform(req.platform());
                token.setDeviceInfo(req.deviceInfo());
                tokenRepo.save(token);
            }
        );

        return ResponseEntity.ok(ApiResponse.ok("Push token registered"));
    }

    @DeleteMapping("/push-tokens/{token}")
    @Transactional
    @Operation(summary = "Deactivate a push token on logout")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable String token) {
        tokenRepo.deactivateByToken(token);
        return ResponseEntity.ok(ApiResponse.ok("Push token deactivated"));
    }
}
