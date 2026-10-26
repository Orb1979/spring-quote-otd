package org.example.quoteotd.repository;

import org.example.quoteotd.model.Viewer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
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
class ViewerRepositoryTest {

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
	private ViewerRepository viewerRepository;

	@Test
	void shouldSaveViewer_withGeneratedId_andFindIt() {
		Viewer saved = viewerRepository.save(new Viewer("Alice"));

		assertThat(saved.getId()).isNotNull();
		assertThat(viewerRepository.findById(saved.getId()))
				.hasValueSatisfying(viewer -> {
					assertThat(viewer.getId()).isEqualTo(saved.getId());
					assertThat(viewer.getName()).isEqualTo("Alice");
				});
	}

	@Test
	void shouldRejectNullName() {
		viewerRepository.save(new Viewer((String) null));

		assertThatThrownBy(() -> viewerRepository.flush())
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}