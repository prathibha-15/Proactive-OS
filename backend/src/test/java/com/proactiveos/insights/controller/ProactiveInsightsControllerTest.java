package com.proactiveos.insights.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import com.proactiveos.events.entity.LifeEventType;
import com.proactiveos.insights.dto.EventTypeCount;
import com.proactiveos.insights.dto.ProactiveInsightsResponse;
import com.proactiveos.insights.dto.Recommendation;
import com.proactiveos.insights.dto.RecommendationCategory;
import com.proactiveos.insights.service.ProactiveInsightsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProactiveInsightsController.class)
class ProactiveInsightsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProactiveInsightsService proactiveInsightsService;

    @Test
    void returnsReadOnlyProactiveInsights() throws Exception {
        when(proactiveInsightsService.getInsights()).thenReturn(new ProactiveInsightsResponse(
                LocalDate.of(2026, 9, 4), LocalDate.of(2026, 10, 1), 5, 2,
            List.of(new EventTypeCount(LifeEventType.STUDY, 3)), List.of(),
            List.of(new Recommendation("data-quality:unknown-event-times", RecommendationCategory.DATA_QUALITY,
                "Some activity times are unknown", "2 logged events have no activity time."))));

        mockMvc.perform(get("/api/insights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.knownTimeEventCount").value(5))
                .andExpect(jsonPath("$.unknownTimeEventCount").value(2))
                .andExpect(jsonPath("$.eventCounts[0].type").value("STUDY"))
                .andExpect(jsonPath("$.recommendations[0].id").value("data-quality:unknown-event-times"))
                .andExpect(jsonPath("$.recommendations[0].category").value("DATA_QUALITY"));
    }
}
