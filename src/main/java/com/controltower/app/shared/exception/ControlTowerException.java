package com.controltower.app.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Base runtime exception for all Control Tower application errors.
 * Carries an HTTP status so the global handler can map it correctly.
 */
public class ControlTowerException extends RuntimeException {

    private final HttpStatus status;

    public ControlTowerException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public ControlTowerException(String message) {
        super(message);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public ControlTowerException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
