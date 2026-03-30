package com.controltower.app.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource does not exist or is not accessible.
 * Maps to HTTP 404.
 */
public class ResourceNotFoundException extends ControlTowerException {

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(String.format("%s not found: %s", resourceName, identifier), HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
