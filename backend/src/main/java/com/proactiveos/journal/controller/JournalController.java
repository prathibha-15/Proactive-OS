package com.proactiveos.journal.controller;

import java.net.URI;
import java.util.List;

import com.proactiveos.journal.dto.JournalRequest;
import com.proactiveos.journal.dto.JournalResponse;
import com.proactiveos.journal.service.JournalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<JournalResponse> create(@Valid @RequestBody JournalRequest request) {
        JournalResponse response = journalService.create(request);
        return ResponseEntity.created(URI.create("/api/journals/" + response.id())).body(response);
    }

    @GetMapping
    public List<JournalResponse> findAll() {
        return journalService.findAll();
    }

    @GetMapping("/{journalId}")
    public JournalResponse findById(@PathVariable Long journalId) {
        return journalService.findById(journalId);
    }

    @PutMapping("/{journalId}")
    public JournalResponse update(@PathVariable Long journalId, @Valid @RequestBody JournalRequest request) {
        return journalService.update(journalId, request);
    }

    @DeleteMapping("/{journalId}")
    public ResponseEntity<Void> delete(@PathVariable Long journalId) {
        journalService.delete(journalId);
        return ResponseEntity.noContent().build();
    }
}