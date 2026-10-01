package com.proactiveos.events.service;

import java.util.List;

import com.proactiveos.events.dto.EventRequest;
import com.proactiveos.events.dto.EventResponse;
import com.proactiveos.events.entity.EventSource;
import com.proactiveos.events.entity.LifeEvent;
import com.proactiveos.events.entity.LifeEventType;
import com.proactiveos.events.repository.LifeEventRepository;
import com.proactiveos.journal.repository.JournalEntryRepository;
import com.proactiveos.journal.service.JournalNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LifeEventService {

    private final LifeEventRepository lifeEventRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final LifeEventMapper mapper;

    public LifeEventService(LifeEventRepository lifeEventRepository,
                             JournalEntryRepository journalEntryRepository,
                             LifeEventMapper mapper) {
        this.lifeEventRepository = lifeEventRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.mapper = mapper;
    }

    @Transactional
    public EventResponse create(EventRequest request) {
        validateJournalReference(request.journalEntryId());
        LifeEvent entity = mapper.toEntity(request);
        return mapper.toResponse(lifeEventRepository.save(entity));
    }

    @Transactional
    public List<EventResponse> replaceJournalEvents(Long journalEntryId, List<EventRequest> requests) {
        validateJournalReference(journalEntryId);
        List<LifeEvent> previousExtractions = lifeEventRepository
                .findAllByJournalEntryIdAndSource(journalEntryId, EventSource.JOURNAL);
        lifeEventRepository.deleteAll(previousExtractions);

        List<LifeEvent> replacements = requests.stream()
                .map(request -> mapper.toEntity(new EventRequest(
                        request.type(), EventSource.JOURNAL, journalEntryId, request.eventTime(), request.confidence(),
                        request.subject(), request.durationMinutes(), request.quantity(), request.unit(),
                        request.description(), request.calories(), request.activityType(), request.count(),
                        request.company(), request.role(), request.status(), request.mood(), request.notes(),
                        request.applicationCount())))
                .toList();
        return lifeEventRepository.saveAll(replacements).stream().map(mapper::toResponse).toList();
    }

    public List<EventResponse> findAll(LifeEventType typeFilter) {
        return lifeEventRepository.findAllByOrderByEventTimeDesc().stream()
                .filter(event -> typeFilter == null || event.getType() == typeFilter)
                .map(mapper::toResponse)
                .toList();
    }

    public EventResponse findById(Long eventId) {
        return mapper.toResponse(findEvent(eventId));
    }

    public List<EventResponse> findByJournal(Long journalEntryId) {
        validateJournalReference(journalEntryId);
        return lifeEventRepository.findAllByJournalEntryIdOrderByEventTimeDesc(journalEntryId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public EventResponse update(Long eventId, EventRequest request) {
        LifeEvent entity = findEvent(eventId);
        if (entity.getType() != request.type()) {
            throw new EventTypeMismatchException(eventId, entity.getType(), request.type());
        }
        validateJournalReference(request.journalEntryId());
        entity.updateCommon(request.journalEntryId(), request.eventTime(), request.source(), request.confidence());
        mapper.applyDetails(entity, request);
        return mapper.toResponse(entity);
    }

    @Transactional
    public void delete(Long eventId) {
        lifeEventRepository.delete(findEvent(eventId));
    }

    private LifeEvent findEvent(Long eventId) {
        return lifeEventRepository.findById(eventId)
                .orElseThrow(() -> new LifeEventNotFoundException(eventId));
    }

    private void validateJournalReference(Long journalEntryId) {
        if (journalEntryId != null && !journalEntryRepository.existsById(journalEntryId)) {
            throw new JournalNotFoundException(journalEntryId);
        }
    }
}
