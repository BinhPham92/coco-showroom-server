package com.cocoshowroom.server.auth.social;

import com.cocoshowroom.server.auth.OAuthProvider;
import com.cocoshowroom.server.shared.InvalidTokenException;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Verifies a Facebook user {@code access_token} using a two-step approach:
 *
 * <ol>
 *   <li><b>debug_token</b> — calls the Graph API {@code /debug_token} endpoint with an
 *       App Access Token ({@code appId|appSecret}) to validate the token and confirm
 *       it was issued to <em>our</em> app ({@code app_id} check). This prevents tokens
 *       minted for other Facebook apps from being accepted.</li>
 *   <li><b>/me</b> — fetches the user's {@code id}, {@code email}, and {@code name} via
 *       the {@code Authorization: Bearer} header so the access token never appears in
 *       a URL and is therefore not captured in server access logs or proxies.</li>
 * </ol>
 *
 * <p><b>Note:</b> the Facebook app must request the {@code email} permission during
 * OAuth so the {@code /me} response includes an email field. If the user has not
 * granted the permission this verifier throws {@link InvalidTokenException}.
 */
@Component
public class FacebookVerifier implements SocialTokenVerifier {

    private static final String GRAPH_BASE    = "https://graph.facebook.com";
    private static final String GRAPH_VERSION = "v18.0";

    private final RestClient restClient;
    private final String     appId;
    private final String     appSecret;

    public FacebookVerifier(
            @Value("${app.oauth.facebook.app-id}")     String appId,
            @Value("${app.oauth.facebook.app-secret}") String appSecret) {
        this.appId     = appId;
        this.appSecret = appSecret;
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
        // ── Step 1: validate token and check app_id ───────────────────────────────
        // The App Access Token (appId|appSecret) is a server-to-server credential
        // that authenticates our call to the debug_token endpoint.
        String appAccessToken = appId + "|" + appSecret;
        DebugTokenResponse debug;
        try {
            debug = restClient.get()
                .uri("/{version}/debug_token?input_token={token}&access_token={appToken}",
                     GRAPH_VERSION, token, appAccessToken)
                .retrieve()
                .body(DebugTokenResponse.class);
        } catch (RestClientException e) {
            throw new InvalidTokenException("Facebook token validation failed: " + e.getMessage());
        }

        if (debug == null || debug.data() == null || !debug.data().isValid()) {
            throw new InvalidTokenException("Facebook token is invalid or expired");
        }

        // Reject tokens issued to a different Facebook application.
        if (!appId.equals(debug.data().appId())) {
            throw new InvalidTokenException(
                "Facebook token was not issued to this application (app_id mismatch)");
        }

        // ── Step 2: fetch profile data — token in Authorization header, NOT in URL ─
        MeResponse me;
        try {
            me = restClient.get()
                .uri("/{version}/me?fields=id,email,name", GRAPH_VERSION)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(MeResponse.class);
        } catch (RestClientException e) {
            throw new InvalidTokenException("Facebook profile fetch failed: " + e.getMessage());
        }

        if (me == null || me.email() == null || me.email().isBlank()) {
            throw new InvalidTokenException(
                "Facebook token did not return an email address. " +
                "Ensure the 'email' permission is granted by the user.");
        }

        return new SocialIdentity(me.email(), me.name(), me.id());
    }

    // ── Response projections ──────────────────────────────────────────────────────

    private record DebugTokenData(
            @JsonProperty("app_id")   String  appId,
            @JsonProperty("is_valid") boolean isValid
    ) {}

    private record DebugTokenResponse(DebugTokenData data) {}

    /** Minimal projection of the Graph API {@code /me} response. */
    private record MeResponse(String id, String email, String name) {}
}
