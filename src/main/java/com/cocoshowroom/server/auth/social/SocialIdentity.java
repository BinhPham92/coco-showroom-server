package com.cocoshowroom.server.auth.social;

/**
 * Normalised identity returned by any {@link SocialTokenVerifier}.
 *
 * @param email          The email address verified by the provider.
 * @param name           Display name from the provider (may be null).
 * @param providerSubject The provider's own stable user ID (Google {@code sub}, Facebook {@code id}).
 */
public record SocialIdentity(String email, String name, String providerSubject) {}
