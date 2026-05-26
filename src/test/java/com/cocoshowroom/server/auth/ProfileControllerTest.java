package com.cocoshowroom.server.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("customer@test.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(Role.CUSTOMER);
        userId = userRepository.save(user).getId();
    }

    // ── GET /v1/auth/me ───────────────────────────────────────────────────────

    @Test
    void me_withToken_returnsProfileIncludingNullFields() throws Exception {
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
        // Set phone first
        mockMvc.perform(patch("/v1/auth/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "phone": "0901234567" }
                    """)
                .with(customerJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.phone").value("0901234567"));

        // Clear it with an empty string
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

    // ── POST /v1/auth/change-password ─────────────────────────────────────────

    @Test
    void changePassword_correctCurrentPassword_returns204() throws Exception {
        mockMvc.perform(post("/v1/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "currentPassword": "password123",
                        "newPassword": "newPassword456"
                    }
                    """)
                .with(customerJwt()))
            .andExpect(status().isNoContent());
    }

    @Test
    void changePassword_newPasswordWorksForSignIn() throws Exception {
        // Change the password
        mockMvc.perform(post("/v1/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "currentPassword": "password123",
                        "newPassword": "newPassword456"
                    }
                    """)
                .with(customerJwt()))
            .andExpect(status().isNoContent());

        // Old password no longer works
        mockMvc.perform(post("/v1/auth/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "email": "customer@test.com", "password": "password123" }
                    """))
            .andExpect(status().isUnauthorized());

        // New password works
        mockMvc.perform(post("/v1/auth/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "email": "customer@test.com", "password": "newPassword456" }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void changePassword_wrongCurrentPassword_returns401() throws Exception {
        mockMvc.perform(post("/v1/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "currentPassword": "wrongPassword",
                        "newPassword": "newPassword456"
                    }
                    """)
                .with(customerJwt()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void changePassword_newPasswordTooShort_returns422() throws Exception {
        mockMvc.perform(post("/v1/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "currentPassword": "password123",
                        "newPassword": "short"
                    }
                    """)
                .with(customerJwt()))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("validation_error"));
    }

    @Test
    void changePassword_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/v1/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "currentPassword": "password123",
                        "newPassword": "newPassword456"
                    }
                    """))
            .andExpect(status().isUnauthorized());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private RequestPostProcessor customerJwt() {
        return jwt().jwt(j -> j
            .subject(userId.toString())
            .claim("role", "CUSTOMER")
        ).authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }
}
