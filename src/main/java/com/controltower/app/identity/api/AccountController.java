package com.controltower.app.identity.api;

import com.controltower.app.identity.domain.User;
import com.controltower.app.identity.domain.UserRepository;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.shared.infrastructure.FileStorageService;
import com.controltower.app.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Tag(name = "Account", description = "Logged-in user profile management")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
public class AccountController {

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public record AvatarRequest(@NotBlank String avatarUrl) {}

    @Operation(summary = "Update profile avatar URL")
    @PutMapping("/avatar")
    public ResponseEntity<ApiResponse<Void>> updateAvatar(
            @RequestBody AvatarRequest req,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setAvatarUrl(req.avatarUrl());
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "Upload profile avatar image")
    @PostMapping(value = "/avatar/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String storageKey = fileStorageService.store(file, "avatars");
        String filename = storageKey.replace("avatars/", "");
        String publicUrl = "/api/v1/public/avatars/" + filename;
        user.setAvatarUrl(publicUrl);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok(publicUrl));
    }
}
