package com.cocoshowroom.server.shared;

/**
 * Thrown when a social provider rejects or cannot verify a submitted token.
 * Mapped to HTTP 401 by {@link GlobalExceptionHandler}.
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
