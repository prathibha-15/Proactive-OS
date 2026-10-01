package com.proactiveos.events.controller;

import java.util.List;

import com.proactiveos.events.dto.EventResponse;
import com.proactiveos.events.service.LifeEventService;
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
    public List<EventResponse> findByJournal(@PathVariable Long journalId) {
        return lifeEventService.findByJournal(journalId);
    }
}
