package com.proactiveos.events.service;

import java.util.List;

import com.proactiveos.auth.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final LifeEventMapper mapper;

    public LifeEventService(LifeEventRepository lifeEventRepository,
                             JournalEntryRepository journalEntryRepository,
                             UserRepository userRepository,
                             LifeEventMapper mapper) {
        this.lifeEventRepository = lifeEventRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    @Transactional
    public EventResponse create(Long ownerId, EventRequest request) {
        validateJournalReference(ownerId, request.journalEntryId());
        LifeEvent entity = mapper.toEntity(withSource(request, EventSource.MANUAL));
        entity.assignOwner(userRepository.getReferenceById(ownerId));
        return mapper.toResponse(lifeEventRepository.save(entity));
    }

    @Transactional
    public List<EventResponse> replaceJournalEvents(Long ownerId, Long journalEntryId, List<EventRequest> requests) {
        validateJournalReference(ownerId, journalEntryId);
        List<LifeEvent> previousExtractions = lifeEventRepository
                .findAllByJournalEntryIdAndSourceAndOwner_Id(journalEntryId, EventSource.JOURNAL, ownerId);
        lifeEventRepository.deleteAll(previousExtractions);

        var owner = userRepository.getReferenceById(ownerId);
        List<LifeEvent> replacements = requests.stream()
                .map(request -> {
                    LifeEvent event = mapper.toEntity(new EventRequest(
                        request.type(), EventSource.JOURNAL, journalEntryId, request.eventTime(), request.confidence(),
                        request.subject(), request.durationMinutes(), request.quantity(), request.unit(),
                        request.description(), request.calories(), request.activityType(), request.count(),
                        request.company(), request.role(), request.status(), request.mood(), request.notes(),
                        request.applicationCount()));
                    event.assignOwner(owner);
                    return event;
                })
                .toList();
        return lifeEventRepository.saveAll(replacements).stream().map(mapper::toResponse).toList();
    }

    public List<EventResponse> findAll(Long ownerId, LifeEventType typeFilter) {
        return lifeEventRepository.findAllByOwner_IdOrderByEventTimeDesc(ownerId).stream()
                .filter(event -> typeFilter == null || event.getType() == typeFilter)
                .map(mapper::toResponse)
                .toList();
    }

    public EventResponse findById(Long ownerId, Long eventId) {
        return mapper.toResponse(findEvent(ownerId, eventId));
    }

    public List<EventResponse> findByJournal(Long ownerId, Long journalEntryId) {
        validateJournalReference(ownerId, journalEntryId);
        return lifeEventRepository.findAllByJournalEntryIdAndOwner_IdOrderByEventTimeDesc(journalEntryId, ownerId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public EventResponse update(Long ownerId, Long eventId, EventRequest request) {
        LifeEvent entity = findEvent(ownerId, eventId);
        if (entity.getType() != request.type()) {
            throw new EventTypeMismatchException(eventId, entity.getType(), request.type());
        }
        validateJournalReference(ownerId, request.journalEntryId());
        entity.updateCommon(request.journalEntryId(), request.eventTime(), entity.getSource(), request.confidence());
        mapper.applyDetails(entity, request);
        return mapper.toResponse(entity);
    }

    @Transactional
    public void delete(Long ownerId, Long eventId) {
        lifeEventRepository.delete(findEvent(ownerId, eventId));
    }

    private LifeEvent findEvent(Long ownerId, Long eventId) {
        return lifeEventRepository.findByIdAndOwner_Id(eventId, ownerId)
                .orElseThrow(() -> new LifeEventNotFoundException(eventId));
    }

    private void validateJournalReference(Long ownerId, Long journalEntryId) {
        if (journalEntryId != null && journalEntryRepository.findByIdAndOwner_Id(journalEntryId, ownerId).isEmpty()) {
            throw new JournalNotFoundException(journalEntryId);
        }
    }

    private EventRequest withSource(EventRequest request, EventSource source) {
        return new EventRequest(
                request.type(), source, request.journalEntryId(), request.eventTime(), request.confidence(),
                request.subject(), request.durationMinutes(), request.quantity(), request.unit(), request.description(),
                request.calories(), request.activityType(), request.count(), request.company(), request.role(),
                request.status(), request.mood(), request.notes(), request.applicationCount());
    }
}
