package com.proactiveos.events.controller;

import java.net.URI;
import java.util.List;

import com.proactiveos.events.dto.EventRequest;
import com.proactiveos.events.dto.EventResponse;
import com.proactiveos.events.entity.LifeEventType;
import com.proactiveos.events.service.LifeEventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class LifeEventController {

    private final LifeEventService lifeEventService;

    public LifeEventController(LifeEventService lifeEventService) {
        this.lifeEventService = lifeEventService;
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(@AuthenticationPrincipal Jwt principal,
                                                @Valid @RequestBody EventRequest request) {
        EventResponse response = lifeEventService.create(userId(principal), request);
        return ResponseEntity.created(URI.create("/api/events/" + response.id())).body(response);
    }

    @GetMapping
    public List<EventResponse> findAll(@AuthenticationPrincipal Jwt principal,
                                       @RequestParam(required = false) LifeEventType type) {
        return lifeEventService.findAll(userId(principal), type);
    }

    @GetMapping("/{eventId}")
    public EventResponse findById(@AuthenticationPrincipal Jwt principal, @PathVariable Long eventId) {
        return lifeEventService.findById(userId(principal), eventId);
    }

    @PutMapping("/{eventId}")
    public EventResponse update(@AuthenticationPrincipal Jwt principal, @PathVariable Long eventId,
                                @Valid @RequestBody EventRequest request) {
        return lifeEventService.update(userId(principal), eventId, request);
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt principal, @PathVariable Long eventId) {
        lifeEventService.delete(userId(principal), eventId);
        return ResponseEntity.noContent().build();
    }

    private Long userId(Jwt principal) {
        return Long.valueOf(principal.getSubject());
    }
}
