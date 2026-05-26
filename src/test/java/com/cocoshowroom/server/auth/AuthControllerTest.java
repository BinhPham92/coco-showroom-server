package com.cocoshowroom.server.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("staff@test.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(Role.STAFF);
        userRepository.save(user);
    }

    @Test
    void signIn_validCredentials_returnsTokenAndUserInfo() throws Exception {
        mockMvc.perform(post("/v1/auth/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "email": "staff@test.com", "password": "password123" }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.email").value("staff@test.com"))
            .andExpect(jsonPath("$.role").value("STAFF"))
            .andExpect(jsonPath("$.userId").isNotEmpty());
    }

    @Test
    void signIn_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/v1/auth/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "email": "staff@test.com", "password": "wrongpassword" }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    @Test
    void signIn_unknownEmail_returns401() throws Exception {
        mockMvc.perform(post("/v1/auth/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "email": "nobody@test.com", "password": "password123" }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    @Test
    void signIn_blankPassword_returns422() throws Exception {
        mockMvc.perform(post("/v1/auth/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "email": "staff@test.com", "password": "" }
                    """))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("validation_error"));
    }

    @Test
    void signOut_returns200() throws Exception {
        mockMvc.perform(post("/v1/auth/sign-out"))
            .andExpect(status().isOk());
    }

    @Test
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/auth/me"))
            .andExpect(status().isUnauthorized());
    }
}
