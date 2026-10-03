package com.proactiveos.journal.repository;

import java.util.List;

import com.proactiveos.journal.entity.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    List<JournalEntry> findAllByOwner_IdOrderByEntryDateDescCreatedAtDesc(Long ownerId);

    java.util.Optional<JournalEntry> findByIdAndOwner_Id(Long id, Long ownerId);
}