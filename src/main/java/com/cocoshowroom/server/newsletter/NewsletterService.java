package com.cocoshowroom.server.newsletter;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NewsletterService {

    private final NewsletterSubscriberRepository repository;

    /**
     * Subscribes an email address.
     *
     * <p>Idempotent — duplicate emails are silently swallowed at the unique
     * constraint level rather than via a check-then-act read, which eliminates
     * the TOCTOU race. No service-level {@code @Transactional} is needed: this
     * is a single insert, so the repository's own transaction is the right
     * boundary. Omitting the outer transaction also prevents the
     * {@code UnexpectedRollbackException} that would occur if we caught a
     * constraint violation inside a {@code rollback-only}-marked outer tx.
     */
    /**
     * Removes an email address from the newsletter list.
     *
     * <p>Idempotent — silently succeeds if the email was never subscribed,
     * so re-clicking an unsubscribe link in an email footer is safe.
     */
    @Transactional
    public void unsubscribe(String email) {
        repository.deleteByEmail(email);
    }

    public void subscribe(String email) {
        try {
            NewsletterSubscriber sub = new NewsletterSubscriber();
            sub.setEmail(email);
            repository.save(sub);
        } catch (DataIntegrityViolationException ignored) {
            // email already subscribed — idempotent, not an error
        }
    }
}
