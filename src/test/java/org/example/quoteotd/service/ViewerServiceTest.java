package org.example.quoteotd.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.quoteotd.dto.ViewerRequest;
import org.example.quoteotd.dto.ViewerResponse;
import org.example.quoteotd.model.Viewer;
import org.example.quoteotd.repository.ViewerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewerServiceTest {

	@Mock
	private ViewerRepository viewerRepository;

	@InjectMocks
	private ViewerService viewerService;

	@Test
	void shouldReturnViewer_whenRepositoryFindsViewer() {
		UUID id = UUID.randomUUID();
		when(viewerRepository.findById(id)).thenReturn(Optional.of(new Viewer(id, "Alice")));

		Optional<ViewerResponse> result = viewerService.findById(id);

		assertThat(result).contains(new ViewerResponse(id, "Alice"));
	}

	@Test
	void shouldReturnEmpty_whenRepositoryHasNoViewer() {
		UUID id = UUID.randomUUID();

		assertThat(viewerService.findById(id)).isEmpty();
	}

	@Test
	void shouldReturnAllViewers_fromRepository() {
		when(viewerRepository.findAll()).thenReturn(List.of(
				new Viewer(UUID.randomUUID(), "Alice"),
				new Viewer(UUID.randomUUID(), "Bob")));

		List<ViewerResponse> result = viewerService.findAll();

		assertThat(result).extracting(ViewerResponse::name)
				.containsExactly("Alice", "Bob");
	}

	@Test
	void shouldSaveNewViewer_andReturnResponse() {
		UUID id = UUID.randomUUID();
		when(viewerRepository.save(any(Viewer.class))).thenReturn(new Viewer(id, "Alice"));

		ViewerResponse result = viewerService.create(new ViewerRequest("Alice"));

		assertThat(result).isEqualTo(new ViewerResponse(id, "Alice"));
		verify(viewerRepository).save(argThat(viewer -> "Alice".equals(viewer.getName())));
	}

	@Test
	void shouldUpdateName_whenViewerExists() {
		UUID id = UUID.randomUUID();
		Viewer existing = new Viewer(id, "Alice");
		when(viewerRepository.findById(id)).thenReturn(Optional.of(existing));
		when(viewerRepository.save(existing)).thenReturn(new Viewer(id, "Alicia"));

		Optional<ViewerResponse> result = viewerService.update(id, new ViewerRequest("Alicia"));

		assertThat(result).contains(new ViewerResponse(id, "Alicia"));
	}

	@Test
	void shouldReturnEmpty_whenUpdatingMissingViewer() {
		UUID id = UUID.randomUUID();
		when(viewerRepository.findById(id)).thenReturn(Optional.empty());

		assertThat(viewerService.update(id, new ViewerRequest("Alicia"))).isEmpty();
		verify(viewerRepository, never()).save(any());
	}

	@Test
	void shouldDeleteViewer_whenItExists() {
		UUID id = UUID.randomUUID();
		when(viewerRepository.existsById(id)).thenReturn(true);

		boolean deleted = viewerService.deleteById(id);

		assertThat(deleted).isTrue();
		verify(viewerRepository).deleteById(id);
	}

	@Test
	void shouldReturnFalse_whenDeletingMissingViewer() {
		UUID id = UUID.randomUUID();
		when(viewerRepository.existsById(id)).thenReturn(false);

		assertThat(viewerService.deleteById(id)).isFalse();
		verify(viewerRepository, never()).deleteById(any());
	}
}
