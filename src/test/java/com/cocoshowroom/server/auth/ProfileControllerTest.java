package com.cocoshowroom.server.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired UserIdentityRepository userIdentityRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userIdentityRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setRole(Role.CUSTOMER);
        userId = userRepository.save(user).getId();

        // Seed the identity so GET /me can resolve email via the service
        UserIdentity identity = new UserIdentity();
        identity.setProvider(OAuthProvider.GOOGLE);
        identity.setEmail("customer@test.com");
        identity.setUser(userRepository.findById(userId).orElseThrow());
        userIdentityRepository.save(identity);
    }

    // ── GET /v1/auth/me ───────────────────────────────────────────────────────

    @Test
    void me_withToken_returnsProfileIncludingNullFields() throws Exception {
        // Email is read from the JWT claim (stored by JwtService at sign-in time)
        mockMvc.perform(get("/v1/auth/me").with(customerJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.email").value("customer@test.com"))
            .andExpect(jsonPath("$.role").value("CUSTOMER"))
            .andExpect(jsonPath("$.name").isEmpty())
            .andExpect(jsonPath("$.phone").isEmpty());
    }

    @Test
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    // ── PATCH /v1/auth/me ─────────────────────────────────────────────────────

    @Test
    void updateProfile_setNameAndPhone_returns200WithUpdatedFields() throws Exception {
        mockMvc.perform(patch("/v1/auth/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "Nguyễn Văn A",
                        "phone": "0901234567"
                    }
                    """)
                .with(customerJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Nguyễn Văn A"))
            .andExpect(jsonPath("$.phone").value("0901234567"));
    }

    @Test
    void updateProfile_onlyName_doesNotClearPhone() throws Exception {
        // Set phone first
        mockMvc.perform(patch("/v1/auth/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "name": "First Name", "phone": "0901234567" }
                    """)
                .with(customerJwt()))
            .andExpect(status().isOk());

        // Update only name — phone should be unchanged
        mockMvc.perform(patch("/v1/auth/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "name": "Updated Name" }
                    """)
                .with(customerJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated Name"))
            .andExpect(jsonPath("$.phone").value("0901234567"));
    }

    @Test
    void updateProfile_emptyPhone_clearsField() throws Exception {
        mockMvc.perform(patch("/v1/auth/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "phone": "0901234567" }
                    """)
                .with(customerJwt()))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/v1/auth/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "phone": "" }
                    """)
                .with(customerJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.phone").isEmpty());
    }

    @Test
    void updateProfile_emptyName_clearsField() throws Exception {
        mockMvc.perform(patch("/v1/auth/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "name": "Nguyễn Văn A" }
                    """)
                .with(customerJwt()))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/v1/auth/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "name": "" }
                    """)
                .with(customerJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").isEmpty());
    }

    @Test
    void updateProfile_invalidPhone_returns422() throws Exception {
        mockMvc.perform(patch("/v1/auth/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "phone": "12345" }
                    """)
                .with(customerJwt()))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("validation_error"));
    }

    @Test
    void updateProfile_unauthenticated_returns401() throws Exception {
        mockMvc.perform(patch("/v1/auth/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "name": "Someone" }
                    """))
            .andExpect(status().isUnauthorized());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private RequestPostProcessor customerJwt() {
        return jwt().jwt(j -> j
            .subject(userId.toString())
            .claim("role", "CUSTOMER")
            .claim("email", "customer@test.com")   // W8: email lives in JWT claim
        ).authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }
}
