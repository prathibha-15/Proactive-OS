package com.proactiveos.journal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.proactiveos.auth.entity.User;
import com.proactiveos.auth.repository.UserRepository;
import com.proactiveos.events.repository.LifeEventRepository;
import com.proactiveos.journal.dto.JournalRequest;
import com.proactiveos.journal.entity.JournalEntry;
import com.proactiveos.journal.repository.JournalEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JournalServiceTest {

    private static final Long OWNER_ID = 7L;

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @Mock
    private LifeEventRepository lifeEventRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JournalService journalService;

    @Test
    void createsJournalWithOriginalContent() {
        JournalEntry entry = JournalEntry.create("I studied Spring Boot for two hours.", LocalDate.now());
        when(journalEntryRepository.save(any(JournalEntry.class))).thenReturn(entry);
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(User.create("owner@example.com", "hash"));

        var response = journalService.create(OWNER_ID, new JournalRequest("I studied Spring Boot for two hours."));

        assertThat(response.content()).isEqualTo("I studied Spring Boot for two hours.");
        assertThat(response.entryDate()).isEqualTo(LocalDate.now());
        verify(journalEntryRepository).save(any(JournalEntry.class));
    }

    @Test
    void retrievesJournal() {
        JournalEntry entry = JournalEntry.create("A journal entry", LocalDate.now());
        when(journalEntryRepository.findByIdAndOwner_Id(1L, OWNER_ID)).thenReturn(Optional.of(entry));

        assertThat(journalService.findById(OWNER_ID, 1L).content()).isEqualTo("A journal entry");
    }

    @Test
    void updatesJournalContent() {
        JournalEntry entry = JournalEntry.create("Before", LocalDate.now());
        when(journalEntryRepository.findByIdAndOwner_Id(1L, OWNER_ID)).thenReturn(Optional.of(entry));

        var response = journalService.update(OWNER_ID, 1L, new JournalRequest("After"));

        assertThat(response.content()).isEqualTo("After");
    }

    @Test
    void deletesJournal() {
        JournalEntry entry = JournalEntry.create("Delete me", LocalDate.now());
        when(journalEntryRepository.findByIdAndOwner_Id(1L, OWNER_ID)).thenReturn(Optional.of(entry));

        journalService.delete(OWNER_ID, 1L);

        verify(journalEntryRepository).delete(entry);
    }

    @Test
    void detachesLifeEventsWithoutDeletingThemWhenJournalIsDeleted() {
        JournalEntry entry = JournalEntry.create("Delete me", LocalDate.now());
        when(journalEntryRepository.findByIdAndOwner_Id(1L, OWNER_ID)).thenReturn(Optional.of(entry));

        journalService.delete(OWNER_ID, 1L);

        verify(lifeEventRepository).detachFromJournal(1L, OWNER_ID);
    }

    @Test
    void returnsJournalList() {
        when(journalEntryRepository.findAllByOwner_IdOrderByEntryDateDescCreatedAtDesc(OWNER_ID))
                .thenReturn(List.of(JournalEntry.create("First", LocalDate.now())));

        assertThat(journalService.findAll(OWNER_ID)).extracting(response -> response.content()).containsExactly("First");
    }

    @Test
    void rejectsMissingJournal() {
        when(journalEntryRepository.findByIdAndOwner_Id(99L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> journalService.findById(OWNER_ID, 99L))
                .isInstanceOf(JournalNotFoundException.class);
    }
}