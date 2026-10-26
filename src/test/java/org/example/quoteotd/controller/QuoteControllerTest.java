package org.example.quoteotd.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.quoteotd.dto.QuoteRequest;
import org.example.quoteotd.dto.QuoteResponse;
import org.example.quoteotd.service.QuoteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuoteController.class)
class QuoteControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private QuoteService quoteService;

	@Test
	void shouldReturnNotFound_whenQuoteDoesNotExist() throws Exception {
		UUID id = UUID.randomUUID();

		when(quoteService.findById(id)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/quotes/{id}", id))
				.andExpect(status().isNotFound());
	}

	@Test
	void shouldReturnQuote_whenQuoteExists() throws Exception {
		UUID id = UUID.randomUUID();
		QuoteResponse quote = new QuoteResponse(id, "Breathe in, breathe out.");

		when(quoteService.findById(id)).thenReturn(Optional.of(quote));

		mockMvc.perform(get("/api/quotes/{id}", id))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").value(id.toString()))
				.andExpect(jsonPath("$.text").value("Breathe in, breathe out."));
	}

	@Test
	void shouldReturnAllQuotes_whenQuotesExist() throws Exception {
		QuoteResponse first = new QuoteResponse(UUID.randomUUID(), "Breathe in, breathe out.");
		QuoteResponse second = new QuoteResponse(UUID.randomUUID(), "Be here now.");

		when(quoteService.findAll()).thenReturn(List.of(first, second));

		mockMvc.perform(get("/api/quotes"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].text").value("Breathe in, breathe out."))
				.andExpect(jsonPath("$[1].text").value("Be here now."));
	}

	@Test
	void shouldReturnCreatedWithLocation_whenQuoteIsCreated() throws Exception {
		QuoteResponse created = new QuoteResponse(UUID.randomUUID(), "Breathe in, breathe out.");

		when(quoteService.create(new QuoteRequest("Breathe in, breathe out."))).thenReturn(created);

		mockMvc.perform(post("/api/quotes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"text\":\"Breathe in, breathe out.\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/quotes/" + created.id()))
				.andExpect(jsonPath("$.id").value(created.id().toString()))
				.andExpect(jsonPath("$.text").value("Breathe in, breathe out."));
	}

	@Test
	void shouldReturnUpdatedQuote_whenQuoteExists() throws Exception {
		UUID id = UUID.randomUUID();
		QuoteResponse updated = new QuoteResponse(id, "Be here now.");

		when(quoteService.update(id, new QuoteRequest("Be here now."))).thenReturn(Optional.of(updated));

		mockMvc.perform(put("/api/quotes/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"text\":\"Be here now.\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id.toString()))
				.andExpect(jsonPath("$.text").value("Be here now."));
	}

	@Test
	void shouldReturnNotFound_whenUpdatedQuoteDoesNotExist() throws Exception {
		UUID id = UUID.randomUUID();

		when(quoteService.update(id, new QuoteRequest("Be here now."))).thenReturn(Optional.empty());

		mockMvc.perform(put("/api/quotes/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"text\":\"Be here now.\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void shouldReturnNoContent_whenQuoteIsDeleted() throws Exception {
		UUID id = UUID.randomUUID();

		when(quoteService.deleteById(id)).thenReturn(true);

		mockMvc.perform(delete("/api/quotes/{id}", id))
				.andExpect(status().isNoContent());
	}

	@Test
	void shouldReturnNotFound_whenDeletedQuoteDoesNotExist() throws Exception {
		UUID id = UUID.randomUUID();

		when(quoteService.deleteById(id)).thenReturn(false);

		mockMvc.perform(delete("/api/quotes/{id}", id))
				.andExpect(status().isNotFound());
	}
}
