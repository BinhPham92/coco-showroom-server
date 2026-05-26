package com.cocoshowroom.server.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the initial STAFF account on first boot if no staff user exists.
 * Credentials are supplied via APP_ADMIN_EMAIL and APP_ADMIN_PASSWORD env vars.
 * The password is BCrypt-hashed at runtime — no pre-computed hash needed.
 * Idempotent: re-running after the user exists is a no-op.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (userRepository.existsByEmail(adminEmail)) {
            return; // Already seeded
        }
        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.STAFF);
        userRepository.save(admin);
        log.info("[seed] Created staff user: {}", adminEmail);
    }
}
