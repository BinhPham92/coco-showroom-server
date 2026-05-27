package com.cocoshowroom.server.admin;

import com.cocoshowroom.server.contact.ContactSubmissionResponse;
import com.cocoshowroom.server.shared.AdminPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Staff-only contact-submission listing.
 *
 * <p>Access to all {@code /v1/admin/**} paths is restricted to {@code ROLE_STAFF}
 * by a blanket rule in {@link com.cocoshowroom.server.config.SecurityConfig}.
 *
 * <p>Example requests:
 * <pre>
 *   GET /v1/admin/contact-submissions          – first page, newest first
 *   GET /v1/admin/contact-submissions?cursor=&lt;token&gt;  – next page
 *   GET /v1/admin/contact-submissions?limit=50 – up to 50 items per page
 * </pre>
 */
@RestController
@RequestMapping("/v1/admin/contact-submissions")
@RequiredArgsConstructor
public class AdminContactController {

    private final AdminContactService adminContactService;

    @GetMapping
    public AdminPageResponse<ContactSubmissionResponse> list(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return adminContactService.list(cursor, limit);
    }
}
