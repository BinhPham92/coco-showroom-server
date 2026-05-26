package com.cocoshowroom.server.auth;

import com.cocoshowroom.server.auth.social.SocialIdentity;
import com.cocoshowroom.server.auth.social.SocialVerifierFactory;
import com.cocoshowroom.server.auth.social.SocialTokenVerifier;
import com.cocoshowroom.server.shared.InvalidTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SocialAuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired UserIdentityRepository userIdentityRepository;

    @MockBean SocialVerifierFactory verifierFactory;

    @BeforeEach
    void setUp() {
        userIdentityRepository.deleteAll();
        userRepository.deleteAll();

        // Google: default throw, then override for known-good tokens.
        // Use doReturn/doThrow to avoid triggering already-registered stubs during setup.
        SocialTokenVerifier googleMock = Mockito.mock(SocialTokenVerifier.class);
        doThrow(new InvalidTokenException("Bad Google token")).when(googleMock).verify(anyString());
        doReturn(new SocialIdentity("alice@gmail.com", "Alice Nguyen", "google-sub-001"))
            .when(googleMock).verify("valid-google-token");
        doReturn(new SocialIdentity("alice@gmail.com", "Alice Nguyen", "google-sub-001"))
            .when(googleMock).verify("returning-google-token");

        // Facebook: same pattern
        SocialTokenVerifier facebookMock = Mockito.mock(SocialTokenVerifier.class);
        doThrow(new InvalidTokenException("Bad Facebook token")).when(facebookMock).verify(anyString());
        doReturn(new SocialIdentity("bob@gmail.com", "Bob Tran", "fb-id-002"))
            .when(facebookMock).verify("valid-facebook-token");

        when(verifierFactory.get(OAuthProvider.GOOGLE)).thenReturn(googleMock);
        when(verifierFactory.get(OAuthProvider.FACEBOOK)).thenReturn(facebookMock);
    }

    // ── New user sign-in (implicit registration) ──────────────────────────────

    @Test
    void social_google_newUser_returns200WithTokenAndIsNewUserTrue() throws Exception {
        mockMvc.perform(post("/v1/auth/social")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "provider": "GOOGLE", "token": "valid-google-token" }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token", notNullValue()))
            .andExpect(jsonPath("$.email", is("alice@gmail.com")))
            .andExpect(jsonPath("$.role", is("CUSTOMER")))
            .andExpect(jsonPath("$.isNewUser", is(true)));
    }

    @Test
    void social_facebook_newUser_returns200WithTokenAndIsNewUserTrue() throws Exception {
        mockMvc.perform(post("/v1/auth/social")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "provider": "FACEBOOK", "token": "valid-facebook-token" }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token", notNullValue()))
            .andExpect(jsonPath("$.email", is("bob@gmail.com")))
            .andExpect(jsonPath("$.isNewUser", is(true)));
    }

    // ── Returning user sign-in ────────────────────────────────────────────────

    @Test
    void social_google_returningUser_isNewUserFalse() throws Exception {
        // First sign-in creates the account
        mockMvc.perform(post("/v1/auth/social")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "provider": "GOOGLE", "token": "valid-google-token" }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isNewUser", is(true)));

        // Second sign-in with the same identity
        mockMvc.perform(post("/v1/auth/social")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "provider": "GOOGLE", "token": "returning-google-token" }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isNewUser", is(false)))
            .andExpect(jsonPath("$.email", is("alice@gmail.com")));
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Test
    void social_invalidToken_returns401() throws Exception {
        mockMvc.perform(post("/v1/auth/social")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "provider": "GOOGLE", "token": "garbage-token" }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("unauthorized")));
    }

    @Test
    void social_missingToken_returns422() throws Exception {
        mockMvc.perform(post("/v1/auth/social")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "provider": "GOOGLE" }
                    """))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code", is("validation_error")));
    }

    @Test
    void social_missingProvider_returns422() throws Exception {
        mockMvc.perform(post("/v1/auth/social")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "token": "valid-google-token" }
                    """))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void social_unknownProvider_returns400() throws Exception {
        mockMvc.perform(post("/v1/auth/social")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "provider": "TWITTER", "token": "some-token" }
                    """))
            .andExpect(status().isBadRequest());
    }

    // ── Sign-out (no-op) ──────────────────────────────────────────────────────

    @Test
    void signOut_returns200() throws Exception {
        mockMvc.perform(post("/v1/auth/sign-out"))
            .andExpect(status().isOk());
    }
}
