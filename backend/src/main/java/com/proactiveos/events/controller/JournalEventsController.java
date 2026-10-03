package com.proactiveos.events.controller;

import java.util.List;

import com.proactiveos.events.dto.EventResponse;
import com.proactiveos.events.service.LifeEventService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/journals/{journalId}/events")
public class JournalEventsController {

    private final LifeEventService lifeEventService;

    public JournalEventsController(LifeEventService lifeEventService) {
        this.lifeEventService = lifeEventService;
    }

    @GetMapping
    public List<EventResponse> findByJournal(@AuthenticationPrincipal Jwt principal, @PathVariable Long journalId) {
        return lifeEventService.findByJournal(Long.valueOf(principal.getSubject()), journalId);
    }
}
