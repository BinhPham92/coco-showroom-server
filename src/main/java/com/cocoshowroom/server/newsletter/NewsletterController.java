package com.cocoshowroom.server.newsletter;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Newsletter subscription endpoint.
 *
 * <p>{@code POST /v1/newsletter} is public. Duplicate emails are silently
 * accepted (idempotent) so the form can be safely re-submitted.
 */
@RestController
@RequestMapping("/v1/newsletter")
@RequiredArgsConstructor
public class NewsletterController {

    private final NewsletterService newsletterService;

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void subscribe(@Valid @RequestBody NewsletterSubscribeRequest request) {
        newsletterService.subscribe(request.email());
    }

    /**
     * Removes an email from the newsletter list. Public so email-footer
     * unsubscribe links work without requiring the user to be signed in.
     * Idempotent — returns 204 even if the email was never subscribed.
     */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(@Valid @RequestBody NewsletterSubscribeRequest request) {
        newsletterService.unsubscribe(request.email());
    }
}
