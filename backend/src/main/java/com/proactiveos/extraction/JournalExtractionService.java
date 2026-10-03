package com.proactiveos.extraction;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;

import com.proactiveos.events.dto.EventRequest;
import com.proactiveos.events.dto.EventResponse;
import com.proactiveos.events.service.LifeEventService;
import com.proactiveos.journal.entity.JournalEntry;
import com.proactiveos.journal.repository.JournalEntryRepository;
import com.proactiveos.journal.service.JournalNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JournalExtractionService {

    private static final Logger log = LoggerFactory.getLogger(JournalExtractionService.class);
    private static final int MAX_RAW_RESPONSE_LOG_LENGTH = 4000;

    private final JournalEntryRepository journalEntryRepository;
    private final AiExtractionService aiExtractionService;
    private final ExtractionResponseParser parser;
    private final LifeEventService lifeEventService;
    private final AiProviderProperties properties;

    @Value("${proactiveos.ai.log-raw-response:false}")
    private boolean logRawResponse;

    public JournalExtractionService(JournalEntryRepository journalEntryRepository,
                                    AiExtractionService aiExtractionService,
                                    ExtractionResponseParser parser,
                                    LifeEventService lifeEventService,
                                    AiProviderProperties properties) {
        this.journalEntryRepository = journalEntryRepository;
        this.aiExtractionService = aiExtractionService;
        this.parser = parser;
        this.lifeEventService = lifeEventService;
        this.properties = properties;
    }

    public List<EventResponse> extract(Long ownerId, Long journalId) {
        JournalEntry journal = journalEntryRepository.findByIdAndOwner_Id(journalId, ownerId)
                .orElseThrow(() -> new JournalNotFoundException(journalId));
        ZoneId timeZone;
        try {
            timeZone = ZoneId.of(properties.timeZone());
        } catch (DateTimeException exception) {
            throw new AiExtractionException("AI extraction time zone configuration is invalid.");
        }

        String stage = "provider";
        try {
            String json = aiExtractionService.extract(journal.getContent(), journal.getEntryDate(), timeZone);
            stage = "validation";
            List<EventRequest> validatedRequests;
            try {
                validatedRequests = parser.parseAndValidate(json, journalId, journal.getEntryDate(), timeZone);
            } catch (AiExtractionException exception) {
                if (logRawResponse) {
                    logRawResponse(json, journal.getContent());
                }
                throw exception;
            }
            stage = "persistence";
            return lifeEventService.replaceJournalEvents(ownerId, journalId, validatedRequests);
        } catch (AiExtractionException exception) {
            log.warn("Journal extraction failed for journal {} during {}: {}", journalId, stage,
                    exception.getClass().getSimpleName());
            throw exception;
        } catch (RuntimeException exception) {
            log.error("Journal extraction failed for journal {} during {}: {}", journalId, stage,
                    exception.getClass().getSimpleName());
            throw new AiExtractionException("Journal extraction failed. No extracted events were saved.", exception);
        }
    }

    private void logRawResponse(String response, String journalContent) {
        String sanitized = response;
        if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
            sanitized = sanitized.replace(properties.apiKey(), "[REDACTED]");
        }
        sanitized = sanitized.replaceAll("(?i)(Bearer\\s+)[^\\s\\\"']+", "$1[REDACTED]");
        sanitized = sanitized.replaceAll("(?im)(authorization\\s*[:=]\\s*)[^\\r\\n]+", "$1[REDACTED]");
        if (journalContent != null && !journalContent.isEmpty()) {
            sanitized = sanitized.replace(journalContent, "[JOURNAL_TEXT_REDACTED]");
        }
        if (sanitized.length() > MAX_RAW_RESPONSE_LOG_LENGTH) {
            sanitized = sanitized.substring(0, MAX_RAW_RESPONSE_LOG_LENGTH) + "...[truncated]";
        }
        log.warn("AI RAW EXTRACTION RESPONSE: {}", sanitized);
    }
}
