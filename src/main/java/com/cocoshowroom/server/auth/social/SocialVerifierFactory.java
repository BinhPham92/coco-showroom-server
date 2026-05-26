package com.cocoshowroom.server.auth.social;

import com.cocoshowroom.server.auth.OAuthProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registry of all {@link SocialTokenVerifier} beans keyed by {@link OAuthProvider}.
 *
 * <p>Spring auto-collects every {@code SocialTokenVerifier} component into the
 * constructor list, so adding a new provider only requires a new
 * {@code @Component} implementation — no changes here.
 */
@Component
public class SocialVerifierFactory {

    private final Map<OAuthProvider, SocialTokenVerifier> registry;

    public SocialVerifierFactory(List<SocialTokenVerifier> verifiers) {
        this.registry = verifiers.stream()
            .collect(Collectors.toMap(SocialTokenVerifier::provider, Function.identity()));
    }

    /**
     * Returns the verifier for the given provider.
     *
     * @throws IllegalArgumentException if no verifier is registered (maps to 400).
     */
    public SocialTokenVerifier get(OAuthProvider provider) {
        SocialTokenVerifier v = registry.get(provider);
        if (v == null) {
            throw new IllegalArgumentException("Unsupported OAuth provider: " + provider);
        }
        return v;
    }
}
