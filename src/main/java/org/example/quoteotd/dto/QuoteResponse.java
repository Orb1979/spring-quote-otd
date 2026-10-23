package org.example.quoteotd.dto;

import java.util.UUID;

public record QuoteResponse(UUID id, String text) {
}
