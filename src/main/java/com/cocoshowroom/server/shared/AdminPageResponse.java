package com.cocoshowroom.server.shared;

import java.util.List;

/**
 * Generic pagination envelope returned by all {@code /v1/admin/**} list endpoints.
 *
 * <p>{@code nextCursor} is a Base64-URL-encoded createdAt timestamp.
 * Pass it as {@code ?cursor=} on the next request to fetch the next page.
 * {@code null} means this is the last page.
 *
 * <p>{@code total} is the total number of matching records across all pages,
 * useful for rendering a badge or page-count indicator in the admin UI.
 */
public record AdminPageResponse<T>(List<T> items, String nextCursor, long total) {}
