package com.cocoshowroom.server.auth.social;

import com.cocoshowroom.server.auth.OAuthProvider;
import com.cocoshowroom.server.shared.InvalidTokenException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Verifies a Facebook user {@code access_token} by calling the Graph API
 * {@code /me?fields=id,email,name} endpoint.
 *
 * <p>Facebook tokens are opaque (not JWTs), so remote verification is the only
 * option. One HTTP call per sign-in; cached implicitly by the short-lived nature
 * of the token — we do not cache server-side in v1.
 *
 * <p><b>Note:</b> the Facebook app must request the {@code email} permission during
 * OAuth so the {@code /me} response includes an email field. If the user has not
 * granted the permission this verifier throws {@link InvalidTokenException}.
 */
@Component
public class FacebookVerifier implements SocialTokenVerifier {

    private static final String GRAPH_BASE = "https://graph.facebook.com";
    private static final String GRAPH_VERSION = "v18.0";

    private final RestClient restClient;

    public FacebookVerifier() {
        this.restClient = RestClient.builder()
            .baseUrl(GRAPH_BASE)
            .build();
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.FACEBOOK;
    }

    @Override
    public SocialIdentity verify(String token) {
        MeResponse me;
        try {
            me = restClient.get()
                .uri("/{version}/me?fields=id,email,name&access_token={token}", GRAPH_VERSION, token)
                .retrieve()
                .body(MeResponse.class);
        } catch (RestClientException e) {
            throw new InvalidTokenException("Facebook token verification failed: " + e.getMessage());
        }

        if (me == null || me.email() == null || me.email().isBlank()) {
            throw new InvalidTokenException(
                "Facebook token did not return an email address. " +
                "Ensure the 'email' permission is granted by the user.");
        }

        return new SocialIdentity(me.email(), me.name(), me.id());
    }

    /** Minimal projection of the Graph API /me response. */
    private record MeResponse(String id, String email, String name) {}
}
