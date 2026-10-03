package com.proactiveos.events.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.time.Instant;
import java.util.List;

import com.proactiveos.events.dto.EventResponse;
import com.proactiveos.events.entity.EventSource;
import com.proactiveos.events.entity.LifeEventType;
import com.proactiveos.events.service.LifeEventService;
import com.proactiveos.journal.service.JournalNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(JournalEventsController.class)
class JournalEventsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LifeEventService lifeEventService;

    @Test
    void returnsEventsForJournal() throws Exception {
        Instant now = Instant.parse("2026-09-24T10:00:00Z");
        var studyEvent = new EventResponse(1L, LifeEventType.STUDY, EventSource.MANUAL, 5L, now, null,
                "Spring Boot", 120, null, null, null, null, null, null, null, null, null, null, null, now, now, null);
        var waterEvent = new EventResponse(2L, LifeEventType.WATER, EventSource.MANUAL, 5L, now, null,
                null, null, 3, null, null, null, null, null, null, null, null, null, null, now, now, null);
        when(lifeEventService.findByJournal(7L, 5L)).thenReturn(List.of(waterEvent, studyEvent));

        mockMvc.perform(get("/api/journals/5/events").with(jwt().jwt(token -> token.subject("7"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void returnsNotFoundForMissingJournal() throws Exception {
        when(lifeEventService.findByJournal(7L, 99L)).thenThrow(new JournalNotFoundException(99L));

        mockMvc.perform(get("/api/journals/99/events").with(jwt().jwt(token -> token.subject("7"))))
                .andExpect(status().isNotFound());
    }
}
