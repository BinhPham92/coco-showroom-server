package com.cocoshowroom.server.auth.social;

import com.cocoshowroom.server.auth.OAuthProvider;

/**
 * Strategy for verifying a social provider's token and extracting a normalised
 * {@link SocialIdentity}.
 *
 * <p>Implementations are Spring beans collected by {@link SocialVerifierFactory}.
 * Adding a new provider = adding a new {@code @Component} implementation; no
 * changes to the controller or service layer are required.
 */
public interface SocialTokenVerifier {

    /** The provider this verifier handles. Used as the registry key. */
    OAuthProvider provider();

    /**
     * Verifies {@code token} with the provider and returns the caller's identity.
     *
     * @throws com.cocoshowroom.server.shared.InvalidTokenException if the token is
     *         invalid, expired, or rejected by the provider.
     */
    SocialIdentity verify(String token);
}
