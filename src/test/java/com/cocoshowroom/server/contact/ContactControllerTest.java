package com.cocoshowroom.server.contact;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContactControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ContactRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void submitContact_validRequest_returns201AndPersists() throws Exception {
        mockMvc.perform(post("/v1/contact")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "Nguyen Van A",
                        "email": "vana@example.com",
                        "subject": "Product enquiry",
                        "message": "I would like to know more about fresh cordyceps."
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.ok").value(true));

        assertThat(repository.count()).isEqualTo(1);
        ContactSubmission saved = repository.findAll().getFirst();
        assertThat(saved.getEmail()).isEqualTo("vana@example.com");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void submitContact_invalidEmail_returns422() throws Exception {
        mockMvc.perform(post("/v1/contact")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "Test",
                        "email": "not-an-email",
                        "subject": "Test",
                        "message": "Test message"
                    }
                    """))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("validation_error"));

        assertThat(repository.count()).isZero();
    }

    @Test
    void submitContact_blankName_returns422() throws Exception {
        mockMvc.perform(post("/v1/contact")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "",
                        "email": "test@example.com",
                        "subject": "Test",
                        "message": "Test message"
                    }
                    """))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("validation_error"));
    }

    @Test
    void submitContact_missingFields_returns422() throws Exception {
        mockMvc.perform(post("/v1/contact")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void submitContact_messageTooLong_returns422() throws Exception {
        String longMessage = "x".repeat(5001);
        mockMvc.perform(post("/v1/contact")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "Test",
                        "email": "test@example.com",
                        "subject": "Test",
                        "message": "%s"
                    }
                    """.formatted(longMessage)))
            .andExpect(status().isUnprocessableEntity());
    }
}
