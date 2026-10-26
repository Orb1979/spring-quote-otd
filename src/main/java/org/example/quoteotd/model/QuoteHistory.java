package org.example.quoteotd.model;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "quote_history")
@Getter
@NoArgsConstructor
public class QuoteHistory {

	@EmbeddedId
	private QuoteHistoryId id;

	@Column(name = "sequence", nullable = false)
	private int sequence;

	@Column(name = "shown_at", nullable = false)
	private Instant shownAt;

	public QuoteHistory(QuoteHistoryId id, int sequence, Instant shownAt) {
		this.id = id;
		this.sequence = sequence;
		this.shownAt = shownAt;
	}
}
