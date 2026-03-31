package com.controltower.app.shared.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Local filesystem storage for ticket attachments.
 * Files are stored under ${app.storage.path}/{prefix}/{uuid}_{originalName}.
 */
@Slf4j
@Service
public class FileStorageService {

    @Value("${app.storage.path:./uploads}")
    private String storagePath;

    /**
     * Stores the uploaded file under the given prefix directory.
     * Returns the storage key (relative path) for later retrieval.
     */
    public String store(MultipartFile file, String prefix) {
        try {
            Path dir = Paths.get(storagePath, prefix);
            Files.createDirectories(dir);

            String storageKey = prefix + "/" + UUID.randomUUID() + "_" + sanitize(file.getOriginalFilename());
            Path target = Paths.get(storagePath, storageKey);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            log.info("Stored file {} as {}", file.getOriginalFilename(), storageKey);
            return storageKey;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store file: " + ex.getMessage(), ex);
        }
    }

    /**
     * Loads a file by its storage key and returns a Resource for streaming download.
     */
    public Resource load(String storageKey) {
        try {
            Path file = Paths.get(storagePath, storageKey);
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("File not found or not readable: " + storageKey);
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Invalid storage key: " + storageKey, ex);
        }
    }

    /**
     * Deletes a stored file by its storage key.
     */
    public void delete(String storageKey) {
        try {
            Path file = Paths.get(storagePath, storageKey);
            Files.deleteIfExists(file);
            log.info("Deleted file {}", storageKey);
        } catch (IOException ex) {
            log.warn("Failed to delete file {}: {}", storageKey, ex.getMessage());
        }
    }

    private String sanitize(String filename) {
        if (filename == null) return "unnamed";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
