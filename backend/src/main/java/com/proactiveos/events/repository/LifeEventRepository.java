package com.proactiveos.events.repository;

import java.util.List;
import java.time.Instant;

import com.proactiveos.events.entity.LifeEvent;
import com.proactiveos.events.entity.EventSource;
import com.proactiveos.integrations.entity.ExternalProviderId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LifeEventRepository extends JpaRepository<LifeEvent, Long> {

    List<LifeEvent> findAllByOwner_IdOrderByEventTimeDesc(Long ownerId);

    List<LifeEvent> findAllByOwner_IdAndEventTimeGreaterThanEqualAndEventTimeLessThan(
            Long ownerId, Instant startInclusive, Instant endExclusive);

    long countByOwner_IdAndEventTimeIsNull(Long ownerId);

    List<LifeEvent> findAllByJournalEntryIdAndOwner_IdOrderByEventTimeDesc(Long journalEntryId, Long ownerId);

    List<LifeEvent> findAllByJournalEntryIdAndSourceAndOwner_Id(Long journalEntryId, EventSource source,
                                                                 Long ownerId);

    java.util.Optional<LifeEvent> findByIdAndOwner_Id(Long id, Long ownerId);

        java.util.Optional<LifeEvent> findByOwner_IdAndExternalProviderAndExternalRecordId(
            Long ownerId, ExternalProviderId provider, String externalRecordId);

    @Modifying
    @Query("update LifeEvent e set e.journalEntryId = null where e.journalEntryId = :journalEntryId and e.owner.id = :ownerId")
    void detachFromJournal(@Param("journalEntryId") Long journalEntryId, @Param("ownerId") Long ownerId);
}
