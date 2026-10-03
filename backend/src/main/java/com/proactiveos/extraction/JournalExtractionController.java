package com.proactiveos.extraction;

import java.util.List;

import com.proactiveos.events.dto.EventResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/journals")
public class JournalExtractionController {

    private final JournalExtractionService journalExtractionService;

    public JournalExtractionController(JournalExtractionService journalExtractionService) {
        this.journalExtractionService = journalExtractionService;
    }

    @PostMapping("/{journalId}/extract")
    public List<EventResponse> extract(@AuthenticationPrincipal Jwt principal, @PathVariable Long journalId) {
        return journalExtractionService.extract(Long.valueOf(principal.getSubject()), journalId);
    }
}
