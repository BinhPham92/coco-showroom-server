package com.cocoshowroom.server.contact;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only view of a {@link ContactSubmission} returned to staff.
 */
public record ContactSubmissionResponse(
        UUID    id,
        String  name,
        String  email,
        String  subject,
        String  message,
        String  ip,
        Instant createdAt
) {
    public static ContactSubmissionResponse from(ContactSubmission c) {
        return new ContactSubmissionResponse(
                c.getId(),
                c.getName(),
                c.getEmail(),
                c.getSubject(),
                c.getMessage(),
                c.getIp(),
                c.getCreatedAt()
        );
    }
}
