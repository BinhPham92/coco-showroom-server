package com.cocoshowroom.server.admin;

import com.cocoshowroom.server.auth.Role;
import com.cocoshowroom.server.auth.User;
import com.cocoshowroom.server.auth.UserRepository;
import com.cocoshowroom.server.contact.ContactRepository;
import com.cocoshowroom.server.contact.ContactSubmission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminContactControllerTest {

    @Autowired MockMvc            mockMvc;
    @Autowired ContactRepository  contactRepository;
    @Autowired UserRepository     userRepository;

    @BeforeEach
    void setUp() {
        contactRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ── Auth ─────────────────────────────────────────────────────────────────

    @Test
    void list_noJwt_returns401() throws Exception {
        mockMvc.perform(get("/v1/admin/contact-submissions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_customerRole_returns403() throws Exception {
        mockMvc.perform(get("/v1/admin/contact-submissions")
                        .with(customerJwt()))
                .andExpect(status().isForbidden());
    }

    // ── Empty state ───────────────────────────────────────────────────────────

    @Test
    void list_noSubmissions_returnsEmptyPage() throws Exception {
        mockMvc.perform(get("/v1/admin/contact-submissions")
                        .with(staffJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.nextCursor").doesNotExist())
                .andExpect(jsonPath("$.total").value(0));
    }

    // ── Full listing ──────────────────────────────────────────────────────────

    @Test
    void list_withSubmissions_returnsAllFields() throws Exception {
        saveSubmission("Alice", "alice@example.com", "Inquiry", "Hello there");

        mockMvc.perform(get("/v1/admin/contact-submissions")
                        .with(staffJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Alice"))
                .andExpect(jsonPath("$.items[0].email").value("alice@example.com"))
                .andExpect(jsonPath("$.items[0].subject").value("Inquiry"))
                .andExpect(jsonPath("$.items[0].message").value("Hello there"))
                .andExpect(jsonPath("$.items[0].id").isNotEmpty())
                .andExpect(jsonPath("$.items[0].createdAt").isNotEmpty());
    }

    @Test
    void list_multipleSubmissions_returnsAll() throws Exception {
        saveSubmission("Alice", "alice@example.com", "Subject A", "Message A");
        saveSubmission("Bob",   "bob@example.com",   "Subject B", "Message B");

        mockMvc.perform(get("/v1/admin/contact-submissions")
                        .with(staffJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.total").value(2));
    }

    // ── Pagination ────────────────────────────────────────────────────────────

    @Test
    void list_limitOne_returnsNextCursor() throws Exception {
        saveSubmission("Alice", "alice@example.com", "S1", "M1");
        saveSubmission("Bob",   "bob@example.com",   "S2", "M2");

        String body = mockMvc.perform(get("/v1/admin/contact-submissions?limit=1")
                        .with(staffJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String cursor = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(body).get("nextCursor").asText();

        mockMvc.perform(get("/v1/admin/contact-submissions?limit=1&cursor=" + cursor)
                        .with(staffJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void saveSubmission(String name, String email, String subject, String message) {
        ContactSubmission cs = new ContactSubmission();
        cs.setName(name);
        cs.setEmail(email);
        cs.setSubject(subject);
        cs.setMessage(message);
        contactRepository.save(cs);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor staffJwt() {
        return jwt().jwt(j -> j
                .subject(UUID.randomUUID().toString())
                .claim("role", "STAFF")
        ).authorities(new SimpleGrantedAuthority("ROLE_STAFF"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor customerJwt() {
        User user = new User();
        user.setRole(Role.CUSTOMER);
        UUID userId = userRepository.save(user).getId();
        return jwt().jwt(j -> j
                .subject(userId.toString())
                .claim("role", "CUSTOMER")
        ).authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }
}
