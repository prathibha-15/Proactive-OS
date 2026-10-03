package com.proactiveos.journal.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.time.Instant;
import java.time.LocalDate;

import com.proactiveos.journal.dto.JournalResponse;
import com.proactiveos.journal.service.JournalNotFoundException;
import com.proactiveos.journal.service.JournalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(JournalController.class)
class JournalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JournalService journalService;

    @Test
    void createsValidJournal() throws Exception {
        when(journalService.create(eq(7L), any())).thenReturn(response());

        mockMvc.perform(post("/api/journals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Today I studied Spring Boot.\"}")
                        .with(jwt().jwt(token -> token.subject("7"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Today I studied Spring Boot."));
    }

    @Test
    void rejectsBlankContent() throws Exception {
        mockMvc.perform(post("/api/journals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"   \"}")
                        .with(jwt().jwt(token -> token.subject("7"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundForMissingJournal() throws Exception {
        when(journalService.findById(7L, 99L)).thenThrow(new JournalNotFoundException(99L));

        mockMvc.perform(get("/api/journals/99").with(jwt().jwt(token -> token.subject("7"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Journal entry not found"));
    }

    @Test
    void rejectsUnauthenticatedJournalReads() throws Exception {
        mockMvc.perform(get("/api/journals")).andExpect(status().isUnauthorized());
    }

    private JournalResponse response() {
        Instant now = Instant.parse("2026-09-23T10:00:00Z");
        return new JournalResponse(1L, "Today I studied Spring Boot.", LocalDate.of(2026, 9, 23), now, now);
    }
}