package com.proactiveos.events.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import com.proactiveos.events.dto.EventResponse;
import com.proactiveos.events.entity.EventSource;
import com.proactiveos.events.entity.LifeEventType;
import com.proactiveos.events.service.LifeEventNotFoundException;
import com.proactiveos.events.service.LifeEventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LifeEventController.class)
class LifeEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LifeEventService lifeEventService;

    @Test
    void createsValidEvent() throws Exception {
        when(lifeEventService.create(any())).thenReturn(studyResponse());

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"STUDY","source":"MANUAL","subject":"Spring Boot","durationMinutes":120}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subject").value("Spring Boot"));
    }

    @Test
    void rejectsInvalidEventData() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"source":"MANUAL","quantity":-1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundForMissingEvent() throws Exception {
        when(lifeEventService.findById(99L)).thenThrow(new LifeEventNotFoundException(99L));

        mockMvc.perform(get("/api/events/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Life event not found"));
    }

    @Test
    void updatesEvent() throws Exception {
        when(lifeEventService.update(eq(1L), any())).thenReturn(studyResponse());

        mockMvc.perform(put("/api/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"STUDY","source":"MANUAL","subject":"Spring Boot","durationMinutes":120}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("Spring Boot"));
    }

    @Test
    void deletesEvent() throws Exception {
        mockMvc.perform(delete("/api/events/1"))
                .andExpect(status().isNoContent());
    }

    private EventResponse studyResponse() {
        Instant now = Instant.parse("2026-09-24T10:00:00Z");
        return new EventResponse(1L, LifeEventType.STUDY, EventSource.MANUAL, null, now, null,
            "Spring Boot", 120, null, null, null, null, null, null, null, null, null, null, null, now, now, null);
    }
}
