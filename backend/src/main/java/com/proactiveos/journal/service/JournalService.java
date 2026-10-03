package com.proactiveos.journal.service;

import java.time.LocalDate;
import java.util.List;

import com.proactiveos.auth.repository.UserRepository;
import com.proactiveos.events.repository.LifeEventRepository;
import com.proactiveos.journal.dto.JournalRequest;
import com.proactiveos.journal.dto.JournalResponse;
import com.proactiveos.journal.entity.JournalEntry;
import com.proactiveos.journal.repository.JournalEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JournalService {

    private final JournalEntryRepository journalEntryRepository;
    private final LifeEventRepository lifeEventRepository;
    private final UserRepository userRepository;

    public JournalService(JournalEntryRepository journalEntryRepository, LifeEventRepository lifeEventRepository,
                          UserRepository userRepository) {
        this.journalEntryRepository = journalEntryRepository;
        this.lifeEventRepository = lifeEventRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public JournalResponse create(Long ownerId, JournalRequest request) {
        JournalEntry entry = JournalEntry.create(request.content(), LocalDate.now(), userRepository.getReferenceById(ownerId));
        return toResponse(journalEntryRepository.save(entry));
    }

    public List<JournalResponse> findAll(Long ownerId) {
        return journalEntryRepository.findAllByOwner_IdOrderByEntryDateDescCreatedAtDesc(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    public JournalResponse findById(Long ownerId, Long journalId) {
        return toResponse(findEntry(ownerId, journalId));
    }

    @Transactional
    public JournalResponse update(Long ownerId, Long journalId, JournalRequest request) {
        JournalEntry entry = findEntry(ownerId, journalId);
        entry.updateContent(request.content());
        return toResponse(entry);
    }

    @Transactional
    public void delete(Long ownerId, Long journalId) {
        JournalEntry entry = findEntry(ownerId, journalId);
        // Detach rather than cascade-delete so manually recorded life events are preserved.
        lifeEventRepository.detachFromJournal(journalId, ownerId);
        journalEntryRepository.delete(entry);
    }

    private JournalEntry findEntry(Long ownerId, Long journalId) {
        return journalEntryRepository.findByIdAndOwner_Id(journalId, ownerId)
                .orElseThrow(() -> new JournalNotFoundException(journalId));
    }

    private JournalResponse toResponse(JournalEntry entry) {
        return new JournalResponse(
                entry.getId(),
                entry.getContent(),
                entry.getEntryDate(),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }
}