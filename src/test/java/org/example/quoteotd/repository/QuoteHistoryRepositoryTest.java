package org.example.quoteotd.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.example.quoteotd.model.Quote;
import org.example.quoteotd.model.QuoteHistory;
import org.example.quoteotd.model.QuoteHistoryId;
import org.example.quoteotd.model.Viewer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class QuoteHistoryRepositoryTest {

	private static final Instant SHOWN_AT = Instant.parse("2026-10-26T12:00:00Z");

	@Autowired private QuoteRepository quoteRepository;
	@Autowired private ViewerRepository viewerRepository;
	@Autowired private QuoteHistoryRepository quoteHistoryRepository;
	private UUID viewerId;

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

	@DynamicPropertySource
	static void datasourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@BeforeEach
	void seedViewer() {
		viewerId = viewerRepository.save(new Viewer("Alice")).getId();
	}

	private UUID seedQuote(String text) {
		return quoteRepository.save(new Quote(text)).getId();
	}

	private void seedHistory(UUID quoteId, int cycle, int sequence) {
		QuoteHistoryId qhId = new QuoteHistoryId(viewerId, quoteId, cycle);
		quoteHistoryRepository.save(new QuoteHistory(qhId, sequence, SHOWN_AT));
		quoteHistoryRepository.flush();
	}

	@Test
	void shouldSaveAndReadHistoryRow_withCompositeKey() {
		UUID quoteId = seedQuote("Breathe in, breathe out.");
		seedHistory(quoteId, 1, 1);

		QuoteHistory found = quoteHistoryRepository.findById(new QuoteHistoryId(viewerId, quoteId, 1)).orElseThrow();
		assertThat(found.getSequence()).isEqualTo(1);
		assertThat(found.getShownAt()).isEqualTo(SHOWN_AT);
	}

	@Test
	void shouldFindLastRow_byHighestSequence_forViewer() {
		UUID firstQuoteId = seedQuote("Breathe in, breathe out.");
		UUID secondQuoteId = seedQuote("Be here now.");
		seedHistory(firstQuoteId, 1, 1);
		seedHistory(secondQuoteId, 1, 2);
		seedHistory(firstQuoteId, 2, 3);

		QuoteHistory last = quoteHistoryRepository.findFirstByIdViewerIdOrderBySequenceDesc(viewerId).orElseThrow();
		assertThat(last.getId().quoteId()).isEqualTo(firstQuoteId);
		assertThat(last.getId().cycle()).isEqualTo(2);
		assertThat(last.getSequence()).isEqualTo(3);
	}

	@Test
	void shouldFindQuoteIds_onlyInGivenCycle() {
		UUID firstQuoteId = seedQuote("Breathe in, breathe out.");
		UUID secondQuoteId = seedQuote("Be here now.");
		seedHistory(firstQuoteId, 1, 1);
		seedHistory(secondQuoteId, 1, 2);
		seedHistory(firstQuoteId, 2, 3);

		List<UUID> quoteIdsInCycleTwo = quoteHistoryRepository.findQuoteIdsInCycle(viewerId, 2);

		assertThat(quoteIdsInCycleTwo).containsExactly(firstQuoteId);
	}

	@Test
	void shouldFindRecentQuoteIds_limitedToPageSize() {
		UUID firstQuoteId = seedQuote("Breathe in, breathe out.");
		UUID secondQuoteId = seedQuote("Be here now.");
		UUID thirdQuoteId = seedQuote("Let it be.");
		UUID fourthQuoteId = seedQuote("This too shall pass.");
		UUID fifthQuoteId = seedQuote("One breath at a time.");
		seedHistory(firstQuoteId, 1, 1);
		seedHistory(secondQuoteId, 1, 2);
		seedHistory(thirdQuoteId, 1, 3);
		seedHistory(fourthQuoteId, 1, 4);
		seedHistory(fifthQuoteId, 1, 5);

		List<UUID> recent = quoteHistoryRepository.findRecentQuoteIds(viewerId, PageRequest.of(0, 2));

		assertThat(recent).containsExactly(fifthQuoteId, fourthQuoteId);
	}

	@Test
	void shouldPruneHistory_keepCurrentCycleAndRowsWithinDistance() {
		UUID oldGoneId = seedQuote("q: oldest, pruned");
		UUID atBoundaryId = seedQuote("q: at boundary, pruned");
		UUID withinDistanceId = seedQuote("q: within K, kept");
		UUID nearEndId = seedQuote("q: end of old cycle, kept");
		UUID currentCycleId = seedQuote("q: current cycle, kept");
		seedHistory(oldGoneId, 1, 1);
		seedHistory(atBoundaryId, 1, 70);
		seedHistory(withinDistanceId, 1, 71);
		seedHistory(nearEndId, 1, 99);
		seedHistory(currentCycleId, 2, 100);

		int deleted = quoteHistoryRepository.pruneHistory(viewerId, 2, 70);

		assertThat(deleted).isEqualTo(2);
		assertThat(quoteHistoryRepository.findAll())
				.extracting(QuoteHistory::getSequence)
				.containsExactlyInAnyOrder(71, 99, 100);
	}

	@Test
	void shouldRejectDuplicateSequence_forSameViewer() {
		UUID firstQuoteId = seedQuote("Breathe in, breathe out.");
		UUID secondQuoteId = seedQuote("Be here now.");

		quoteHistoryRepository.save(new QuoteHistory(new QuoteHistoryId(viewerId, firstQuoteId, 1), 5, SHOWN_AT));
		quoteHistoryRepository.save(new QuoteHistory(new QuoteHistoryId(viewerId, secondQuoteId, 1), 5, SHOWN_AT));

		assertThatThrownBy(() -> quoteHistoryRepository.flush()).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void shouldCascadeDeleteHistory_whenViewerDeleted() {
		UUID quoteId = seedQuote("Breathe in, breathe out.");
		seedHistory(quoteId, 1, 1);

		viewerRepository.deleteById(viewerId);
		viewerRepository.flush(); // Force the pending DELETE to the DB so the schema's ON DELETE CASCADE runs

		assertThat(quoteHistoryRepository.count()).isZero();
	}

	@Test
	void shouldCascadeDeleteHistory_whenQuoteDeleted() {
		UUID quoteId = seedQuote("Breathe in, breathe out.");
		seedHistory(quoteId, 1, 1);

		quoteRepository.deleteById(quoteId);
		quoteRepository.flush(); // Force the pending DELETE to the DB so the schema's ON DELETE CASCADE runs

		assertThat(quoteHistoryRepository.count()).isZero();
	}
}
