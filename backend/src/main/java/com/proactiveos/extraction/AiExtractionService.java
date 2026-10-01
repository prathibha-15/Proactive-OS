package com.proactiveos.extraction;

import java.time.LocalDate;
import java.time.ZoneId;

public interface AiExtractionService {

    String extract(String journalContent, LocalDate entryDate, ZoneId timeZone);
}
