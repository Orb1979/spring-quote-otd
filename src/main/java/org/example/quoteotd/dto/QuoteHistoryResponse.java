package org.example.quoteotd.dto;

import java.time.Instant;
import java.util.UUID;

public record QuoteHistoryResponse(UUID viewerId, UUID quoteId, int cycle, int sequence, Instant shownAt) {
}
