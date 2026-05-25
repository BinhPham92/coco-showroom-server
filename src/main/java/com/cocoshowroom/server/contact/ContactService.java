package com.cocoshowroom.server.contact;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactService {

    private final ContactRepository repository;

    @Transactional
    public void submit(ContactRequest request, String ip) {
        ContactSubmission submission = new ContactSubmission();
        submission.setName(request.name());
        submission.setEmail(request.email());
        submission.setSubject(request.subject());
        submission.setMessage(request.message());
        submission.setIp(ip);

        repository.save(submission);
        log.info("[contact] saved id={} email={}", submission.getId(), request.email());
    }
}
