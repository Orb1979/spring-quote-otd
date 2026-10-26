package org.example.quoteotd.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.example.quoteotd.dto.QuoteResponse;
import org.example.quoteotd.model.Quote;
import org.example.quoteotd.model.Viewer;
import org.example.quoteotd.repository.QuoteHistoryRepository;
import org.example.quoteotd.repository.QuoteRepository;
import org.example.quoteotd.repository.ViewerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Testcontainers
class NextQuoteServiceIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

	// Autowire the datasource properties from the container to Spring Boot
	@DynamicPropertySource
	static void datasourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@Autowired
	private NextQuoteService nextQuoteService;

	@Autowired
	private QuoteRepository quoteRepository;

	@Autowired
	private ViewerRepository viewerRepository;

	@Autowired
	private QuoteHistoryRepository quoteHistoryRepository;

	@Test
	void shouldCycleQuotes_withoutRepeatWithinCycle_andRespectDistanceAtCycleBoundary() {
		UUID viewerId = viewerRepository.save(new Viewer("Alice")).getId();
		quoteRepository.save(new Quote("Breathe in, breathe out."));
		quoteRepository.save(new Quote("Be here now."));
		quoteRepository.save(new Quote("Let it be."));

		List<UUID> shown = new ArrayList<>();
		for (int i = 0; i < 7; i++) {
			shown.add(nextQuoteService.nextQuote(viewerId).id());
		}

		// first cycle: three distinct quotes, then the second cycle: three distinct again
		assertThat(shown.subList(0, 3)).doesNotHaveDuplicates();
		assertThat(shown.subList(3, 6)).doesNotHaveDuplicates();
		// distance rule across each boundary: K >= 1 keeps the last quote away at the start of the next cycle
		assertThat(shown.get(3)).isNotEqualTo(shown.get(2));
		assertThat(shown.get(6)).isNotEqualTo(shown.get(5));
		// early sequences are all within K_MAX, so nothing is pruned yet
		assertThat(quoteHistoryRepository.count()).isEqualTo(7);
	}

	@Test
	void shouldShowSingleQuote_repeatedly_whenItIsTheOnlyOne() {
		UUID viewerId = viewerRepository.save(new Viewer("Bob")).getId();
		quoteRepository.save(new Quote("Breathe."));

		QuoteResponse first = nextQuoteService.nextQuote(viewerId);
		QuoteResponse second = nextQuoteService.nextQuote(viewerId);

		// n = 1 means K = 0: the quote is just shown again, in a new cycle
		assertThat(first.id()).isEqualTo(second.id());
		assertThat(quoteHistoryRepository.count()).isEqualTo(2);
	}
}
