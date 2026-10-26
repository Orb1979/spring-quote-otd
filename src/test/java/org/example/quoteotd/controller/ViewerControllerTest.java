package org.example.quoteotd.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.quoteotd.dto.ViewerRequest;
import org.example.quoteotd.dto.ViewerResponse;
import org.example.quoteotd.service.ViewerService;
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

@WebMvcTest(ViewerController.class)
class ViewerControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ViewerService viewerService;

	@Test
	void shouldReturnViewer_whenViewerExists() throws Exception {
		UUID id = UUID.randomUUID();
		ViewerResponse viewer = new ViewerResponse(id, "Alice");

		when(viewerService.findById(id)).thenReturn(Optional.of(viewer));

		mockMvc.perform(get("/api/viewers/{id}", id))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").value(id.toString()))
				.andExpect(jsonPath("$.name").value("Alice"));
	}

	@Test
	void shouldReturnNotFound_whenViewerDoesNotExist() throws Exception {
		UUID id = UUID.randomUUID();

		when(viewerService.findById(id)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/viewers/{id}", id))
				.andExpect(status().isNotFound());
	}

	@Test
	void shouldReturnAllViewers_whenViewersExist() throws Exception {
		ViewerResponse first = new ViewerResponse(UUID.randomUUID(), "Alice");
		ViewerResponse second = new ViewerResponse(UUID.randomUUID(), "Bob");

		when(viewerService.findAll()).thenReturn(List.of(first, second));

		mockMvc.perform(get("/api/viewers"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].name").value("Alice"))
				.andExpect(jsonPath("$[1].name").value("Bob"));
	}

	@Test
	void shouldReturnCreatedWithLocation_whenViewerIsCreated() throws Exception {
		ViewerResponse created = new ViewerResponse(UUID.randomUUID(), "Alice");

		when(viewerService.create(new ViewerRequest("Alice"))).thenReturn(created);

		mockMvc.perform(post("/api/viewers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Alice\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/viewers/" + created.id()))
				.andExpect(jsonPath("$.id").value(created.id().toString()))
				.andExpect(jsonPath("$.name").value("Alice"));
	}

	@Test
	void shouldReturnUpdatedViewer_whenViewerExists() throws Exception {
		UUID id = UUID.randomUUID();
		ViewerResponse updated = new ViewerResponse(id, "Alicia");

		when(viewerService.update(id, new ViewerRequest("Alicia"))).thenReturn(Optional.of(updated));

		mockMvc.perform(put("/api/viewers/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Alicia\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id.toString()))
				.andExpect(jsonPath("$.name").value("Alicia"));
	}

	@Test
	void shouldReturnNotFound_whenUpdatedViewerDoesNotExist() throws Exception {
		UUID id = UUID.randomUUID();

		when(viewerService.update(id, new ViewerRequest("Alicia"))).thenReturn(Optional.empty());

		mockMvc.perform(put("/api/viewers/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Alicia\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void shouldReturnNoContent_whenViewerIsDeleted() throws Exception {
		UUID id = UUID.randomUUID();

		when(viewerService.deleteById(id)).thenReturn(true);

		mockMvc.perform(delete("/api/viewers/{id}", id))
				.andExpect(status().isNoContent());
	}

	@Test
	void shouldReturnNotFound_whenDeletedViewerDoesNotExist() throws Exception {
		UUID id = UUID.randomUUID();

		when(viewerService.deleteById(id)).thenReturn(false);

		mockMvc.perform(delete("/api/viewers/{id}", id))
				.andExpect(status().isNotFound());
	}
}
