package com.cocoshowroom.server.shared;

/**
 * Standard error envelope — matches the shape the frontend's fetcher.ts expects:
 * { code, message, traceId }
 */
public record ApiErrorResponse(
    String code,
    String message,
    String traceId
) {}
