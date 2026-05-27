package com.cocoshowroom.server.auth.social;

import com.cocoshowroom.server.auth.OAuthProvider;
import com.cocoshowroom.server.shared.InvalidTokenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Verifies a Google {@code id_token} (RS256 JWT) against Google's public JWKS.
 *
 * <p>Validation steps performed by Nimbus (expiry, signature) plus our own
 * audience and issuer checks so a token minted for a different app can't be reused.
 */
@Component
public class GoogleVerifier implements SocialTokenVerifier {

    private static final String GOOGLE_JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";

    private final NimbusJwtDecoder decoder;
    private final String clientId;

    public GoogleVerifier(@Value("${app.oauth.google.client-id}") String clientId) {
        this.clientId = clientId;
        this.decoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWKS_URI).build();
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public SocialIdentity verify(String token) {
        Jwt jwt;
        try {
            jwt = decoder.decode(token);
        } catch (JwtException e) {
            throw new InvalidTokenException("Google token invalid: " + e.getMessage());
        }

        // Audience must include our client ID
        var aud = jwt.getAudience();
        if (aud == null || !aud.contains(clientId)) {
            throw new InvalidTokenException("Google token audience mismatch");
        }

        // Issuer must be Google (two valid forms)
        String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : "";
        if (!issuer.equals("https://accounts.google.com") && !issuer.equals("accounts.google.com")) {
            throw new InvalidTokenException("Google token issuer invalid");
        }

        // Only verified emails are trusted; an unverified address could be claimed by
        // anyone who registers with that email on a permissive provider.
        Boolean emailVerified = jwt.getClaim("email_verified");
        if (!Boolean.TRUE.equals(emailVerified)) {
            throw new InvalidTokenException("Google account email is not verified");
        }

        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new InvalidTokenException("Google token missing email claim");
        }

        return new SocialIdentity(email, jwt.getClaimAsString("name"), jwt.getSubject());
    }
}
