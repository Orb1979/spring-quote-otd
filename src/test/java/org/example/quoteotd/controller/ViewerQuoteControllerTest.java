package org.example.quoteotd.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.example.quoteotd.dto.QuoteHistoryResponse;
import org.example.quoteotd.dto.QuoteResponse;
import org.example.quoteotd.exception.NoQuotesException;
import org.example.quoteotd.exception.ViewerNotFoundException;
import org.example.quoteotd.service.NextQuoteService;
import org.example.quoteotd.service.QuoteHistoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ViewerQuoteController.class)
class ViewerQuoteControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private NextQuoteService nextQuoteService;

	@MockitoBean
	private QuoteHistoryService quoteHistoryService;

	@Test
	void shouldReturnNextQuote_withJson() throws Exception {
		UUID viewerId = UUID.randomUUID();
		QuoteResponse next = new QuoteResponse(UUID.randomUUID(), "Breathe in, breathe out.");

		when(nextQuoteService.nextQuote(viewerId)).thenReturn(next);

		mockMvc.perform(post("/api/viewers/{id}/quotes/next", viewerId))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").value(next.id().toString()))
				.andExpect(jsonPath("$.text").value("Breathe in, breathe out."));
	}

	@Test
	void shouldReturnProblemDetail_whenViewerDoesNotExist() throws Exception {
		UUID viewerId = UUID.randomUUID();

		when(nextQuoteService.nextQuote(viewerId)).thenThrow(new ViewerNotFoundException(viewerId));

		mockMvc.perform(post("/api/viewers/{id}/quotes/next", viewerId))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Viewer not found"))
				.andExpect(jsonPath("$.detail").value("Viewer " + viewerId + " does not exist"));
	}

	@Test
	void shouldReturnProblemDetail_whenNoQuotesExist() throws Exception {
		UUID viewerId = UUID.randomUUID();

		when(nextQuoteService.nextQuote(viewerId)).thenThrow(new NoQuotesException());

		mockMvc.perform(post("/api/viewers/{id}/quotes/next", viewerId))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("No quotes available"));
	}

	@Test
	void shouldReturnHistory_newestFirst() throws Exception {
		UUID viewerId = UUID.randomUUID();
		Instant shownAt = Instant.parse("2026-10-26T12:00:00Z");

		when(quoteHistoryService.findHistory(viewerId)).thenReturn(List.of(
				new QuoteHistoryResponse(viewerId, UUID.randomUUID(), 2, 5, shownAt),
				new QuoteHistoryResponse(viewerId, UUID.randomUUID(), 2, 4, shownAt)));

		mockMvc.perform(get("/api/viewers/{id}/history", viewerId))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].sequence").value(5))
				.andExpect(jsonPath("$[0].cycle").value(2))
				.andExpect(jsonPath("$[0].shownAt").value("2026-10-26T12:00:00Z"))
				.andExpect(jsonPath("$[1].sequence").value(4));
	}

	@Test
	void shouldReturnProblemDetail_whenHistoryViewerDoesNotExist() throws Exception {
		UUID viewerId = UUID.randomUUID();

		when(quoteHistoryService.findHistory(viewerId)).thenThrow(new ViewerNotFoundException(viewerId));

		mockMvc.perform(get("/api/viewers/{id}/history", viewerId))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Viewer not found"));
	}
}
