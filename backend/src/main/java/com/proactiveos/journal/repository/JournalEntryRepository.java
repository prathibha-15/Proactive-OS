package com.proactiveos.journal.repository;

import java.util.List;

import com.proactiveos.journal.entity.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    List<JournalEntry> findAllByOrderByEntryDateDescCreatedAtDesc();
}