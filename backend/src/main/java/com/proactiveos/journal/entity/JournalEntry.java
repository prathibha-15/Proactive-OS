package com.proactiveos.journal.entity;

import java.time.Instant;
import java.time.LocalDate;

import com.proactiveos.auth.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "journal_entries")
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User owner;

    protected JournalEntry() {
    }

    private JournalEntry(String content, LocalDate entryDate, User owner) {
        this.content = content;
        this.entryDate = entryDate;
        this.owner = owner;
    }

    public static JournalEntry create(String content, LocalDate entryDate) {
        return new JournalEntry(content, entryDate, null);
    }

    public static JournalEntry create(String content, LocalDate entryDate, User owner) {
        return new JournalEntry(content, entryDate, owner);
    }

    @PrePersist
    void initializeTimestamps() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    public void updateContent(String content) {
        this.content = content;
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public User getOwner() {
        return owner;
    }
}