package com.proactiveos.extraction;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.proactiveos.events.dto.EventResponse;
import com.proactiveos.events.entity.EventSource;
import com.proactiveos.events.entity.LifeEventType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(JournalExtractionController.class)
class JournalExtractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JournalExtractionService journalExtractionService;

    @Test
    void triggersExtractionForJournal() throws Exception {
        when(journalExtractionService.extract(42L)).thenReturn(List.of(new EventResponse(
                1L, LifeEventType.STUDY, EventSource.JOURNAL, 42L, null, null, "Spring Boot", 120,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null)));

        mockMvc.perform(post("/api/journals/42/extract"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("STUDY"))
                .andExpect(jsonPath("$[0].source").value("JOURNAL"))
                .andExpect(jsonPath("$[0].journalEntryId").value(42));
    }
}
