package com.proactiveos.integrations.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import com.proactiveos.auth.config.SecurityConfiguration;
import com.proactiveos.events.entity.LifeEventType;
import com.proactiveos.integrations.dto.ExternalActivityRecord;
import com.proactiveos.integrations.dto.IntegrationProviderStatus;
import com.proactiveos.integrations.dto.IntegrationSyncResponse;
import com.proactiveos.integrations.entity.ExternalProviderId;
import com.proactiveos.integrations.service.ExternalDataSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExternalIntegrationsController.class)
@Import(SecurityConfiguration.class)
class ExternalIntegrationsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExternalDataSyncService syncService;

    @Test
    void returnsCurrentUsersAvailableDevelopmentProvider() throws Exception {
        when(syncService.getProviders(12L)).thenReturn(List.of(
            new IntegrationProviderStatus(ExternalProviderId.MOCK, "Development mock provider", true,
                List.of(LifeEventType.STEPS, LifeEventType.SLEEP, LifeEventType.WORKOUT), null),
            new IntegrationProviderStatus(ExternalProviderId.HEALTH_CONNECT,
                "Health Connect (Android companion required)", false, true,
                List.of(LifeEventType.STEPS, LifeEventType.SLEEP), null)));

        mockMvc.perform(get("/api/integrations").with(jwt().jwt(token -> token.subject("12"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].provider").value("MOCK"))
                .andExpect(jsonPath("$[0].developmentOnly").value(true))
                .andExpect(jsonPath("$[0].lastSyncedAt").doesNotExist())
                .andExpect(jsonPath("$[1].provider").value("HEALTH_CONNECT"))
                .andExpect(jsonPath("$[1].clientUploadRequired").value(true))
                .andExpect(jsonPath("$[1].developmentOnly").value(false));

        verify(syncService).getProviders(12L);
    }

    @Test
    void syncUsesTheAuthenticatedSubjectAndReturnsCounts() throws Exception {
        when(syncService.sync(34L, ExternalProviderId.MOCK)).thenReturn(
                new IntegrationSyncResponse(ExternalProviderId.MOCK, 5, 4, 0, 1,
                        Instant.parse("2026-10-03T12:00:00Z")));

        mockMvc.perform(post("/api/integrations/MOCK/sync").with(jwt().jwt(token -> token.subject("34"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("MOCK"))
                .andExpect(jsonPath("$.eventsFetched").value(5))
                .andExpect(jsonPath("$.eventsCreated").value(4))
                .andExpect(jsonPath("$.eventsSkipped").value(1));

        verify(syncService).sync(34L, ExternalProviderId.MOCK);
    }

        @Test
        void healthConnectUploadUsesJwtOwnerAndIgnoresClientOwnerId() throws Exception {
        var record = new ExternalActivityRecord("steps-2026-10-03", LifeEventType.STEPS,
            Instant.parse("2026-10-03T04:00:00Z"), null, 8247, null, null);
        when(syncService.sync(34L, ExternalProviderId.HEALTH_CONNECT, List.of(record))).thenReturn(
            new IntegrationSyncResponse(ExternalProviderId.HEALTH_CONNECT, 1, 1, 0, 0,
                Instant.parse("2026-10-03T12:00:00Z")));

        mockMvc.perform(post("/api/integrations/HEALTH_CONNECT/sync")
                .with(jwt().jwt(token -> token.subject("34")))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {"ownerId":999,"records":[{"externalRecordId":"steps-2026-10-03",
                    "type":"STEPS","eventTime":"2026-10-03T04:00:00Z","count":8247}]}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.provider").value("HEALTH_CONNECT"))
            .andExpect(jsonPath("$.eventsCreated").value(1));

        verify(syncService).sync(34L, ExternalProviderId.HEALTH_CONNECT, List.of(record));
        }

        @Test
        void rejectsMalformedHealthConnectTimestamp() throws Exception {
        mockMvc.perform(post("/api/integrations/HEALTH_CONNECT/sync")
                .with(jwt().jwt(token -> token.subject("34")))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {"records":[{"externalRecordId":"steps-invalid-time","type":"STEPS",
                    "eventTime":"not-a-timestamp","count":12}]}
                    """))
            .andExpect(status().isBadRequest());
        }

    @Test
    void rejectsClientSuppliedRecordsForMockProvider() throws Exception {
            var record = new ExternalActivityRecord("mock-forgery", LifeEventType.STEPS,
                null, null, 12, null, null);
            when(syncService.sync(34L, ExternalProviderId.MOCK, List.of(record)))
                .thenThrow(new com.proactiveos.integrations.service.InvalidExternalActivityException(
                    "This provider does not accept client-supplied records."));

            mockMvc.perform(post("/api/integrations/MOCK/sync")
                    .with(jwt().jwt(token -> token.subject("34")))
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content("{\"records\":[{\"externalRecordId\":\"mock-forgery\",\"type\":\"STEPS\",\"count\":12}]}"))
                .andExpect(status().isBadGateway());
            }

    @Test
    void rejectsUnauthenticatedStatusAndSyncRequests() throws Exception {
        mockMvc.perform(get("/api/integrations")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/integrations/MOCK/sync")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/integrations/HEALTH_CONNECT/sync")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"records\":[]}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsUnsupportedProvider() throws Exception {
        mockMvc.perform(post("/api/integrations/GARMIN/sync")
                        .with(jwt().jwt(token -> token.subject("34"))))
                .andExpect(status().isBadRequest());
    }
}