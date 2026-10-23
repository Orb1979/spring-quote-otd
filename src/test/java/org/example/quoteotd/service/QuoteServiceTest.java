package org.example.quoteotd.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.quoteotd.dto.QuoteRequest;
import org.example.quoteotd.dto.QuoteResponse;
import org.example.quoteotd.model.Quote;
import org.example.quoteotd.repository.QuoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class QuoteServiceTest {

	@Mock
	private QuoteRepository quoteRepository;

	@InjectMocks
	private QuoteService quoteService;

	@Test
	void shouldReturnQuote_whenRepositoryFindsQuote() {
		UUID id = UUID.randomUUID();
		given(quoteRepository.findById(id)).willReturn(Optional.of(new Quote(id, "Breathe in, breathe out.")));

		Optional<QuoteResponse> result = quoteService.findById(id);

		assertThat(result).contains(new QuoteResponse(id, "Breathe in, breathe out."));
	}

	@Test
	void shouldReturnEmpty_whenRepositoryHasNoQuote() {
		UUID id = UUID.randomUUID();

		assertThat(quoteService.findById(id)).isEmpty();
	}

	@Test
	void shouldReturnAllQuotes_fromRepository() {
		given(quoteRepository.findAll()).willReturn(List.of(
				new Quote(UUID.randomUUID(), "Breathe in, breathe out."),
				new Quote(UUID.randomUUID(), "Be here now.")));

		List<QuoteResponse> result = quoteService.findAll();

		assertThat(result).extracting(QuoteResponse::text)
				.containsExactly("Breathe in, breathe out.", "Be here now.");
	}

	@Test
	void shouldSaveNewQuote_andReturnResponse() {
		UUID id = UUID.randomUUID();
		given(quoteRepository.save(any(Quote.class))).willReturn(new Quote(id, "Breathe in, breathe out."));

		QuoteResponse result = quoteService.create(new QuoteRequest("Breathe in, breathe out."));

		assertThat(result).isEqualTo(new QuoteResponse(id, "Breathe in, breathe out."));
		then(quoteRepository).should().save(argThat(quote -> "Breathe in, breathe out.".equals(quote.getText())));
	}

	@Test
	void shouldUpdateText_whenQuoteExists() {
		UUID id = UUID.randomUUID();
		Quote existing = new Quote(id, "old text");
		given(quoteRepository.findById(id)).willReturn(Optional.of(existing));
		given(quoteRepository.save(existing)).willReturn(new Quote(id, "new text"));

		Optional<QuoteResponse> result = quoteService.update(id, new QuoteRequest("new text"));

		assertThat(result).contains(new QuoteResponse(id, "new text"));
	}

	@Test
	void shouldReturnEmpty_whenUpdatingMissingQuote() {
		UUID id = UUID.randomUUID();
		given(quoteRepository.findById(id)).willReturn(Optional.empty());

		assertThat(quoteService.update(id, new QuoteRequest("new text"))).isEmpty();
		then(quoteRepository).should(never()).save(any());
	}

	@Test
	void shouldDeleteQuote_whenItExists() {
		UUID id = UUID.randomUUID();
		given(quoteRepository.existsById(id)).willReturn(true);

		boolean deleted = quoteService.deleteById(id);

		assertThat(deleted).isTrue();
		then(quoteRepository).should().deleteById(id);
	}

	@Test
	void shouldReturnFalse_whenDeletingMissingQuote() {
		UUID id = UUID.randomUUID();
		given(quoteRepository.existsById(id)).willReturn(false);

		assertThat(quoteService.deleteById(id)).isFalse();
		then(quoteRepository).should(never()).deleteById(any());
	}
}
