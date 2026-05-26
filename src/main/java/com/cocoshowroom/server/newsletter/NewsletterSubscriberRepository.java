package com.cocoshowroom.server.newsletter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NewsletterSubscriberRepository extends JpaRepository<NewsletterSubscriber, UUID> {

    boolean existsByEmail(String email);

    void deleteByEmail(String email);
}
