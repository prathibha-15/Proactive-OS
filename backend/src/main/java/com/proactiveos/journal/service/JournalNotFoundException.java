package com.proactiveos.journal.service;

public class JournalNotFoundException extends RuntimeException {

    public JournalNotFoundException(Long journalId) {
        super("Journal entry %d was not found.".formatted(journalId));
    }
}