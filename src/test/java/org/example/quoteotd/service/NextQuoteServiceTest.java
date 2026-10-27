package org.example.quoteotd.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.quoteotd.config.QuoteProperties;
import org.example.quoteotd.exception.NoQuotesException;
import org.example.quoteotd.exception.ViewerNotFoundException;
import org.example.quoteotd.dto.QuoteResponse;
import org.example.quoteotd.model.Quote;
import org.example.quoteotd.model.QuoteHistory;
import org.example.quoteotd.model.QuoteHistoryId;
import org.example.quoteotd.model.Viewer;
import org.example.quoteotd.repository.QuoteHistoryRepository;
import org.example.quoteotd.repository.QuoteRepository;
import org.example.quoteotd.repository.ViewerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NextQuoteServiceTest {

	@Mock private QuoteRepository quoteRepository;
	@Mock private QuoteHistoryRepository quoteHistoryRepository;
	@Mock private ViewerRepository viewerRepository;
	@Mock private java.util.Random random;

	private static final QuoteProperties PROPERTIES = new QuoteProperties(50);
	private static final Instant SHOWN_AT = Instant.parse("2026-10-26T12:00:00Z");

	private NextQuoteService nextQuoteService;
	private UUID viewerId;

	@BeforeEach
	void setUp() {
		viewerId = UUID.randomUUID();
		nextQuoteService = new NextQuoteService(quoteRepository, quoteHistoryRepository,
				viewerRepository, random, PROPERTIES);
	}

	@ParameterizedTest
	@CsvSource({
			"1, 50, 0",
			"2, 50, 1",
			"10, 50, 5",
			"99, 50, 49",
			"100, 50, 50",
			"200, 50, 50",
			"10, 4, 4"
	})
	void shouldComputeK_fromQuoteCount(int n, int kMax, int expectedK) {
		assertThat(NextQuoteService.computeK(n, kMax)).isEqualTo(expectedK);
	}

	@Test
	void shouldThrowViewerNotFound_whenViewerDoesNotExist() {
		when(viewerRepository.lockById(viewerId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> nextQuoteService.nextQuote(viewerId))
				.isInstanceOf(ViewerNotFoundException.class);
	}

	@Test
	void shouldThrowNoQuotes_whenNoQuotesExist() {
		when(viewerRepository.lockById(viewerId)).thenReturn(Optional.of(new Viewer(viewerId, "Alice")));
		when(quoteRepository.findAll()).thenReturn(List.of());

		assertThatThrownBy(() -> nextQuoteService.nextQuote(viewerId))
				.isInstanceOf(NoQuotesException.class);
	}

	@Test
	void shouldShowFirstQuote_withCycleOneSequenceOne_forNewViewer() {
		Quote first = new Quote(UUID.randomUUID(), "Breathe in, breathe out.");
		Quote second = new Quote(UUID.randomUUID(), "Be here now.");

		when(viewerRepository.lockById(viewerId)).thenReturn(Optional.of(new Viewer(viewerId, "Alice")));
		when(quoteRepository.findAll()).thenReturn(List.of(first, second));
		when(quoteHistoryRepository.findFirstByIdViewerIdOrderBySequenceDesc(viewerId)).thenReturn(Optional.empty());
		when(random.nextInt(2)).thenReturn(0);

		QuoteResponse result = nextQuoteService.nextQuote(viewerId);

		assertThat(result).isEqualTo(new QuoteResponse(first.getId(), "Breathe in, breathe out."));
		QuoteHistory saved = captureSavedHistory();
		assertThat(saved.getId()).isEqualTo(new QuoteHistoryId(viewerId, first.getId(), 1));
		assertThat(saved.getSequence()).isEqualTo(1);
		assertThat(saved.getShownAt()).isNotNull();
		verify(quoteHistoryRepository).pruneHistory(viewerId, 1, 1 - 50);
	}

	@Test
	void shouldStartNewCycle_whenAllQuotesWereSeenInCurrentCycle() {
		Quote first = new Quote(UUID.randomUUID(), "Breathe in, breathe out.");
		Quote second = new Quote(UUID.randomUUID(), "Be here now.");
		QuoteHistory last = new QuoteHistory(new QuoteHistoryId(viewerId, second.getId(), 1), 2, SHOWN_AT);

		when(viewerRepository.lockById(viewerId)).thenReturn(Optional.of(new Viewer(viewerId, "Alice")));
		when(quoteRepository.findAll()).thenReturn(List.of(first, second));
		when(quoteHistoryRepository.findFirstByIdViewerIdOrderBySequenceDesc(viewerId)).thenReturn(Optional.of(last));
		when(quoteHistoryRepository.findQuoteIdsInCycle(viewerId, 1))
				.thenReturn(List.of(first.getId(), second.getId()));
		when(quoteHistoryRepository.findRecentQuoteIds(eq(viewerId), any(PageRequest.class)))
				.thenReturn(List.of(second.getId()));
		when(random.nextInt(1)).thenReturn(0);

		QuoteResponse result = nextQuoteService.nextQuote(viewerId);

		// K = 1 excludes the last shown quote, so the new cycle starts with the other one
		assertThat(result.id()).isEqualTo(first.getId());
		QuoteHistory saved = captureSavedHistory();
		assertThat(saved.getId()).isEqualTo(new QuoteHistoryId(viewerId, first.getId(), 2));
		assertThat(saved.getSequence()).isEqualTo(3);
	}

	@Test
	void shouldExcludeQuotesSeenInCurrentCycle_andRecentlyShown() {
		Quote first = new Quote(UUID.randomUUID(), "Breathe in, breathe out.");
		Quote second = new Quote(UUID.randomUUID(), "Be here now.");
		Quote third = new Quote(UUID.randomUUID(), "Let it be.");
		QuoteHistory last = new QuoteHistory(new QuoteHistoryId(viewerId, second.getId(), 1), 2, SHOWN_AT);

		when(viewerRepository.lockById(viewerId)).thenReturn(Optional.of(new Viewer(viewerId, "Alice")));
		when(quoteRepository.findAll()).thenReturn(List.of(first, second, third));
		when(quoteHistoryRepository.findFirstByIdViewerIdOrderBySequenceDesc(viewerId)).thenReturn(Optional.of(last));
		when(quoteHistoryRepository.findQuoteIdsInCycle(viewerId, 1))
				.thenReturn(List.of(first.getId(), second.getId()));
		when(quoteHistoryRepository.findRecentQuoteIds(eq(viewerId), any(PageRequest.class)))
				.thenReturn(List.of(second.getId()));
		when(random.nextInt(1)).thenReturn(0);

		QuoteResponse result = nextQuoteService.nextQuote(viewerId);

		assertThat(result.id()).isEqualTo(third.getId());
		QuoteHistory saved = captureSavedHistory();
		assertThat(saved.getId()).isEqualTo(new QuoteHistoryId(viewerId, third.getId(), 1));
		assertThat(saved.getSequence()).isEqualTo(3);
	}

	@Test
	void shouldPruneOldHistory_usingKMax_notK() {
		Quote first = new Quote(UUID.randomUUID(), "Breathe in, breathe out.");
		Quote second = new Quote(UUID.randomUUID(), "Be here now.");
		QuoteHistory last = new QuoteHistory(new QuoteHistoryId(viewerId, second.getId(), 2), 100, SHOWN_AT);

		when(viewerRepository.lockById(viewerId)).thenReturn(Optional.of(new Viewer(viewerId, "Alice")));
		when(quoteRepository.findAll()).thenReturn(List.of(first, second));
		when(quoteHistoryRepository.findFirstByIdViewerIdOrderBySequenceDesc(viewerId)).thenReturn(Optional.of(last));
		when(quoteHistoryRepository.findQuoteIdsInCycle(viewerId, 2))
				.thenReturn(List.of(first.getId(), second.getId()));
		when(quoteHistoryRepository.findRecentQuoteIds(eq(viewerId), any(PageRequest.class)))
				.thenReturn(List.of(second.getId()));
		when(random.nextInt(1)).thenReturn(0);

		nextQuoteService.nextQuote(viewerId);

		// K = 1 for n = 2, but pruning must always use K_MAX = 50: bound is 101 - 50 = 51, not 101 - 1 = 100
		verify(quoteHistoryRepository).pruneHistory(viewerId, 3, 51);
	}

	@Test
	void shouldFailFast_whenNoQuoteIsEligible() {
		// Unreachable with consistent data: K < N always holds, and the last-K window
		// always overlaps the current cycle's rows, so something is always eligible.
		// Fires only on a broken K formula or inconsistent history data — fail loudly
		// here rather than crash in random.nextInt(0).
		Quote first = new Quote(UUID.randomUUID(), "Breathe in, breathe out.");
		Quote second = new Quote(UUID.randomUUID(), "Be here now.");

		when(viewerRepository.lockById(viewerId)).thenReturn(Optional.of(new Viewer(viewerId, "Alice")));
		when(quoteRepository.findAll()).thenReturn(List.of(first, second));
		when(quoteHistoryRepository.findFirstByIdViewerIdOrderBySequenceDesc(viewerId)).thenReturn(Optional.empty());
		when(quoteHistoryRepository.findQuoteIdsInCycle(viewerId, 1)).thenReturn(List.of(first.getId()));
		when(quoteHistoryRepository.findRecentQuoteIds(eq(viewerId), any(PageRequest.class)))
				.thenReturn(List.of(second.getId()));

		assertThatThrownBy(() -> nextQuoteService.nextQuote(viewerId))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining(viewerId.toString());
	}

	private QuoteHistory captureSavedHistory() {
		ArgumentCaptor<QuoteHistory> captor = ArgumentCaptor.forClass(QuoteHistory.class);
		verify(quoteHistoryRepository).save(captor.capture());
		return captor.getValue();
	}
}
