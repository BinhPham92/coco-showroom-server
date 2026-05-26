package com.cocoshowroom.server.shared;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiErrorResponse handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return new ApiErrorResponse("validation_error", message, traceId(req));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String message = "Invalid value for parameter '" + ex.getName() + "': " + ex.getValue();
        return new ApiErrorResponse("bad_request", message, traceId(req));
    }

    /**
     * Re-throw so Spring Security's {@code ExceptionTranslationFilter} converts it
     * to a proper 403 response. Without this the catch-all below would return 500.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public void handleAccessDenied(AccessDeniedException ex) throws AccessDeniedException {
        throw ex;
    }

    /**
     * Jackson can't deserialize an unknown enum value — surface as 400 rather than 500.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return new ApiErrorResponse("bad_request", "Malformed or unreadable request body", traceId(req));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return new ApiErrorResponse("bad_request", ex.getMessage(), traceId(req));
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleNotFound(NotFoundException ex, HttpServletRequest req) {
        return new ApiErrorResponse("not_found", ex.getMessage(), traceId(req));
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleConflict(ConflictException ex, HttpServletRequest req) {
        return new ApiErrorResponse("conflict", ex.getMessage(), traceId(req));
    }

    @ExceptionHandler(InvalidTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleInvalidToken(InvalidTokenException ex, HttpServletRequest req) {
        // Return a generic message — never leak provider-specific rejection details to clients
        return new ApiErrorResponse("unauthorized", "Social token verification failed", traceId(req));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("[error] traceId={} {}", traceId(req), ex.getMessage(), ex);
        return new ApiErrorResponse("internal_error", "An unexpected error occurred.", traceId(req));
    }

    private String traceId(HttpServletRequest req) {
        String id = req.getHeader("X-Request-Id");
        return (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
    }
}
