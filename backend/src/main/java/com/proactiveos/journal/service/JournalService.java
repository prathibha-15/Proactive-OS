package com.proactiveos.journal.service;

import java.time.LocalDate;
import java.util.List;

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

    public JournalService(JournalEntryRepository journalEntryRepository, LifeEventRepository lifeEventRepository) {
        this.journalEntryRepository = journalEntryRepository;
        this.lifeEventRepository = lifeEventRepository;
    }

    @Transactional
    public JournalResponse create(JournalRequest request) {
        JournalEntry entry = JournalEntry.create(request.content(), LocalDate.now());
        return toResponse(journalEntryRepository.save(entry));
    }

    public List<JournalResponse> findAll() {
        return journalEntryRepository.findAllByOrderByEntryDateDescCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    public JournalResponse findById(Long journalId) {
        return toResponse(findEntry(journalId));
    }

    @Transactional
    public JournalResponse update(Long journalId, JournalRequest request) {
        JournalEntry entry = findEntry(journalId);
        entry.updateContent(request.content());
        return toResponse(entry);
    }

    @Transactional
    public void delete(Long journalId) {
        JournalEntry entry = findEntry(journalId);
        // Detach rather than cascade-delete so manually recorded life events are preserved.
        lifeEventRepository.detachFromJournal(journalId);
        journalEntryRepository.delete(entry);
    }

    private JournalEntry findEntry(Long journalId) {
        return journalEntryRepository.findById(journalId)
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