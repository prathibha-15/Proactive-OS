package com.proactiveos.integrations.controller;

import java.util.List;

import com.proactiveos.integrations.dto.IntegrationProviderStatus;
import com.proactiveos.integrations.dto.IntegrationSyncResponse;
import com.proactiveos.integrations.entity.ExternalProviderId;
import com.proactiveos.integrations.service.ExternalDataSyncService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integrations")
public class ExternalIntegrationsController {

    private final ExternalDataSyncService syncService;

    public ExternalIntegrationsController(ExternalDataSyncService syncService) {
        this.syncService = syncService;
    }

    @GetMapping
    public List<IntegrationProviderStatus> getProviders(@AuthenticationPrincipal Jwt principal) {
        return syncService.getProviders(ownerId(principal));
    }

    @PostMapping("/{provider}/sync")
    public IntegrationSyncResponse sync(@AuthenticationPrincipal Jwt principal,
                                        @PathVariable ExternalProviderId provider) {
        return syncService.sync(ownerId(principal), provider);
    }

    private Long ownerId(Jwt principal) {
        return Long.valueOf(principal.getSubject());
    }
}