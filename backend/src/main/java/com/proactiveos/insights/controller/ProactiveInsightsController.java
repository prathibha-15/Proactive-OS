package com.proactiveos.insights.controller;

import com.proactiveos.insights.dto.ProactiveInsightsResponse;
import com.proactiveos.insights.service.ProactiveInsightsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insights")
public class ProactiveInsightsController {

    private final ProactiveInsightsService proactiveInsightsService;

    public ProactiveInsightsController(ProactiveInsightsService proactiveInsightsService) {
        this.proactiveInsightsService = proactiveInsightsService;
    }

    @GetMapping
    public ProactiveInsightsResponse getInsights() {
        return proactiveInsightsService.getInsights();
    }
}
