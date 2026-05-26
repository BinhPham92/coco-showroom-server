package com.cocoshowroom.server.review;

public enum ReviewStatus {
    /** Awaiting staff approval — not visible to the public. */
    PENDING,
    /** Approved by staff — visible in the public product listing. */
    APPROVED,
    /** Rejected by staff — not visible, permanently excluded. */
    REJECTED
}
