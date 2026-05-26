package com.cocoshowroom.server.newsletter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NewsletterControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired NewsletterSubscriberRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void subscribe_validEmail_returns204AndPersists() throws Exception {
        mockMvc.perform(post("/v1/newsletter")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "email": "nguyen@example.com" }
                    """))
            .andExpect(status().isNoContent());

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void subscribe_duplicateEmail_isIdempotentReturns204() throws Exception {
        mockMvc.perform(post("/v1/newsletter")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "email": "twice@example.com" }
                    """))
            .andExpect(status().isNoContent());

        // Second call — must not fail or create a duplicate row
        mockMvc.perform(post("/v1/newsletter")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "email": "twice@example.com" }
                    """))
            .andExpect(status().isNoContent());

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void subscribe_invalidEmail_returns422() throws Exception {
        mockMvc.perform(post("/v1/newsletter")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "email": "not-an-email" }
                    """))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("validation_error"));
    }

    @Test
    void subscribe_blankEmail_returns422() throws Exception {
        mockMvc.perform(post("/v1/newsletter")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "email": "" }
                    """))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("validation_error"));
    }

    // ── DELETE /v1/newsletter ─────────────────────────────────────────────────

    @Test
    void unsubscribe_existingEmail_returns204AndRemoves() throws Exception {
        // Subscribe first
        mockMvc.perform(post("/v1/newsletter")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "email": "leave@example.com" }
                    """))
            .andExpect(status().isNoContent());

        assertThat(repository.count()).isEqualTo(1);

        // Unsubscribe
        mockMvc.perform(delete("/v1/newsletter")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "email": "leave@example.com" }
                    """))
            .andExpect(status().isNoContent());

        assertThat(repository.count()).isEqualTo(0);
    }

    @Test
    void unsubscribe_unknownEmail_isIdempotentReturns204() throws Exception {
        mockMvc.perform(delete("/v1/newsletter")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "email": "never@example.com" }
                    """))
            .andExpect(status().isNoContent());
    }

    @Test
    void unsubscribe_invalidEmail_returns422() throws Exception {
        mockMvc.perform(delete("/v1/newsletter")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "email": "not-valid" }
                    """))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("validation_error"));
    }
}
