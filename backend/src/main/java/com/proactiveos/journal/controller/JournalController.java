package com.proactiveos.journal.controller;

import java.net.URI;
import java.util.List;

import com.proactiveos.journal.dto.JournalRequest;
import com.proactiveos.journal.dto.JournalResponse;
import com.proactiveos.journal.service.JournalService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/journals")
public class JournalController {

    private final JournalService journalService;

    public JournalController(JournalService journalService) {
        this.journalService = journalService;
    }

    @PostMapping
    public ResponseEntity<JournalResponse> create(@AuthenticationPrincipal Jwt principal,
                                                  @Valid @RequestBody JournalRequest request) {
        JournalResponse response = journalService.create(userId(principal), request);
        return ResponseEntity.created(URI.create("/api/journals/" + response.id())).body(response);
    }

    @GetMapping
    public List<JournalResponse> findAll(@AuthenticationPrincipal Jwt principal) {
        return journalService.findAll(userId(principal));
    }

    @GetMapping("/{journalId}")
    public JournalResponse findById(@AuthenticationPrincipal Jwt principal, @PathVariable Long journalId) {
        return journalService.findById(userId(principal), journalId);
    }

    @PutMapping("/{journalId}")
    public JournalResponse update(@AuthenticationPrincipal Jwt principal, @PathVariable Long journalId,
                                  @Valid @RequestBody JournalRequest request) {
        return journalService.update(userId(principal), journalId, request);
    }

    @DeleteMapping("/{journalId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt principal, @PathVariable Long journalId) {
        journalService.delete(userId(principal), journalId);
        return ResponseEntity.noContent().build();
    }

    private Long userId(Jwt principal) {
        return Long.valueOf(principal.getSubject());
    }
}