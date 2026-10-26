package org.example.quoteotd.service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.example.quoteotd.config.QuoteProperties;
import org.example.quoteotd.dto.QuoteResponse;
import org.example.quoteotd.exception.NoQuotesException;
import org.example.quoteotd.exception.ViewerNotFoundException;
import org.example.quoteotd.model.Quote;
import org.example.quoteotd.model.QuoteHistory;
import org.example.quoteotd.model.QuoteHistoryId;
import org.example.quoteotd.repository.QuoteHistoryRepository;
import org.example.quoteotd.repository.QuoteRepository;
import org.example.quoteotd.repository.ViewerRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NextQuoteService {

	private final QuoteRepository quoteRepository;
	private final QuoteHistoryRepository quoteHistoryRepository;
	private final ViewerRepository viewerRepository;
	private final Random random;
	private final QuoteProperties properties;

	public NextQuoteService(QuoteRepository quoteRepository, QuoteHistoryRepository quoteHistoryRepository,
			ViewerRepository viewerRepository, Random random, QuoteProperties properties) {
		this.quoteRepository = quoteRepository;
		this.quoteHistoryRepository = quoteHistoryRepository;
		this.viewerRepository = viewerRepository;
		this.random = random;
		this.properties = properties;
	}

	/**
	 * Shows the viewer their next quote: never one already seen in the current
	 * cycle, never one of the last K shown overall (which keeps the start of a
	 * new cycle fresh). When every quote has been seen in the current cycle,
	 * a new cycle starts. The viewer row is locked so concurrent requests for
	 * the same viewer serialize.
	 */
	@Transactional
	public QuoteResponse nextQuote(UUID viewerId) {
		if (viewerRepository.lockById(viewerId).isEmpty()) {
			throw new ViewerNotFoundException(viewerId);
		}
		List<Quote> quotes = quoteRepository.findAll();
		if (quotes.isEmpty()) {
			throw new NoQuotesException();
		}
		int n = quotes.size();

		QuoteHistory last = quoteHistoryRepository
				.findFirstByIdViewerIdOrderBySequenceDesc(viewerId)
				.orElse(null);
		int cycle = last == null ? 1 : last.getId().cycle();
		int sequence = last == null ? 0 : last.getSequence();

		Set<UUID> seenInCurrentCycle = new HashSet<>(quoteHistoryRepository.findQuoteIdsInCycle(viewerId, cycle));
		if (seenInCurrentCycle.size() >= n) {
			cycle++;
			seenInCurrentCycle.clear();
		}

		int k = computeK(n, properties.kMax());
		Set<UUID> recentlyShown = k == 0
				? Set.of()
				: new HashSet<>(quoteHistoryRepository.findRecentQuoteIds(viewerId, PageRequest.of(0, k)));

		List<Quote> eligible = quotes.stream()
				.filter(quote -> !seenInCurrentCycle.contains(quote.getId()))
				.filter(quote -> !recentlyShown.contains(quote.getId()))
				.toList();
		if (eligible.isEmpty()) {
			throw new IllegalStateException("No eligible quotes for viewer " + viewerId);
		}

		Quote chosen = eligible.get(random.nextInt(eligible.size()));
		int nextSequence = sequence + 1;
		quoteHistoryRepository.save(new QuoteHistory(
				new QuoteHistoryId(viewerId, chosen.getId(), cycle), nextSequence, Instant.now()));

		// Prune always with K_MAX, never K, so rows within the distance rule survive
		quoteHistoryRepository.pruneHistory(viewerId, cycle, nextSequence - properties.kMax());

		return new QuoteResponse(chosen.getId(), chosen.getText());
	}

	static int computeK(int n, int kMax) {
		return Math.min(n / 2, kMax);
	}
}
