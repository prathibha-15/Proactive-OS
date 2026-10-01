package com.proactiveos.events.repository;

import java.util.List;

import com.proactiveos.events.entity.LifeEvent;
import com.proactiveos.events.entity.EventSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LifeEventRepository extends JpaRepository<LifeEvent, Long> {

    List<LifeEvent> findAllByOrderByEventTimeDesc();

    List<LifeEvent> findAllByJournalEntryIdOrderByEventTimeDesc(Long journalEntryId);

    List<LifeEvent> findAllByJournalEntryIdAndSource(Long journalEntryId, EventSource source);

    @Modifying
    @Query("update LifeEvent e set e.journalEntryId = null where e.journalEntryId = :journalEntryId")
    void detachFromJournal(@Param("journalEntryId") Long journalEntryId);
}
