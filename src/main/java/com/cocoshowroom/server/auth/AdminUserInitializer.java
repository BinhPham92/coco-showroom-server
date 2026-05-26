package com.cocoshowroom.server.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the initial STAFF account on first boot if no identity exists for the
 * configured (provider, email) pair.
 *
 * <p>Set {@code ADMIN_PROVIDER} (e.g. {@code google}) and {@code ADMIN_EMAIL}
 * (e.g. {@code boss@example.com}) before first boot. The first time that OAuth
 * account signs in, it matches this pre-seeded identity and receives a STAFF JWT.
 *
 * <p>Idempotent: re-running after the identity exists is a no-op.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserInitializer {

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.provider}")
    private String adminProvider;   // e.g. "google" or "facebook"

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        OAuthProvider provider = OAuthProvider.valueOf(adminProvider.toUpperCase());

        if (userIdentityRepository.existsByProviderAndEmail(provider, adminEmail)) {
            return; // Already seeded — idempotent
        }

        User admin = new User();
        admin.setRole(Role.STAFF);
        admin = userRepository.save(admin);

        UserIdentity identity = new UserIdentity();
        identity.setProvider(provider);
        identity.setEmail(adminEmail);
        identity.setUser(admin);
        userIdentityRepository.save(identity);

        log.info("[seed] Created staff identity: {}:{}", provider, adminEmail);
    }
}
