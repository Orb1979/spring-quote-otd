package org.example.quoteotd.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.example.quoteotd.dto.QuoteHistoryResponse;
import org.example.quoteotd.exception.ViewerNotFoundException;
import org.example.quoteotd.model.QuoteHistory;
import org.example.quoteotd.model.QuoteHistoryId;
import org.example.quoteotd.repository.QuoteHistoryRepository;
import org.example.quoteotd.repository.ViewerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuoteHistoryServiceTest {

	@Mock
	private QuoteHistoryRepository quoteHistoryRepository;

	@Mock
	private ViewerRepository viewerRepository;

	@InjectMocks
	private QuoteHistoryService quoteHistoryService;

	private static final Instant SHOWN_AT = Instant.parse("2026-10-26T12:00:00Z");

	@Test
	void shouldReturnHistoryRows_newestFirst() {
		UUID viewerId = UUID.randomUUID();
		when(viewerRepository.existsById(viewerId)).thenReturn(true);
		when(quoteHistoryRepository.findAllByIdViewerIdOrderBySequenceDesc(viewerId)).thenReturn(List.of(
				new QuoteHistory(new QuoteHistoryId(viewerId, UUID.randomUUID(), 2), 5, SHOWN_AT),
				new QuoteHistory(new QuoteHistoryId(viewerId, UUID.randomUUID(), 1), 2, SHOWN_AT)));

		List<QuoteHistoryResponse> history = quoteHistoryService.findHistory(viewerId);

		assertThat(history).extracting(QuoteHistoryResponse::sequence).containsExactly(5, 2);
		assertThat(history).extracting(QuoteHistoryResponse::cycle).containsExactly(2, 1);
		assertThat(history).allSatisfy(row -> assertThat(row.viewerId()).isEqualTo(viewerId));
	}

	@Test
	void shouldReturnEmptyHistory_whenViewerHasNoHistory() {
		UUID viewerId = UUID.randomUUID();
		when(viewerRepository.existsById(viewerId)).thenReturn(true);
		when(quoteHistoryRepository.findAllByIdViewerIdOrderBySequenceDesc(viewerId)).thenReturn(List.of());

		assertThat(quoteHistoryService.findHistory(viewerId)).isEmpty();
	}

	@Test
	void shouldThrowViewerNotFound_whenViewerDoesNotExist() {
		UUID viewerId = UUID.randomUUID();
		when(viewerRepository.existsById(viewerId)).thenReturn(false);

		assertThatThrownBy(() -> quoteHistoryService.findHistory(viewerId))
				.isInstanceOf(ViewerNotFoundException.class);
	}
}
