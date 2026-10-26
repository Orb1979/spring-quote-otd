package org.example.quoteotd.model;

import java.io.Serializable;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record QuoteHistoryId(
		@Column(name = "viewer_id", nullable = false) UUID viewerId,
		@Column(name = "quote_id", nullable = false) UUID quoteId,
		@Column(name = "cycle", nullable = false) int cycle) implements Serializable {
}
