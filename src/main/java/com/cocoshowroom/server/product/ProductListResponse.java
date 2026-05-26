package com.cocoshowroom.server.product;

import java.util.List;

/**
 * Cursor-paginated product list.
 * nextCursor is null when there are no more pages.
 */
public record ProductListResponse(List<ProductResponse> items, String nextCursor) {}
