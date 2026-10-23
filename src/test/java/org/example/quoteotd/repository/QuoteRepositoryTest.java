package org.example.quoteotd.repository;

import org.example.quoteotd.model.Quote;
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
class QuoteRepositoryTest {

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
	private QuoteRepository quoteRepository;

	@Test
	void shouldSaveQuote_withGeneratedId_andFindIt() {
		Quote saved = quoteRepository.save(new Quote("Breathe in, breathe out."));

		assertThat(saved.getId()).isNotNull();
		assertThat(quoteRepository.findById(saved.getId()))
				.hasValueSatisfying(quote -> {
					assertThat(quote.getId()).isEqualTo(saved.getId());
					assertThat(quote.getText()).isEqualTo("Breathe in, breathe out.");
				});
	}

	@Test
	void shouldRejectDuplicateQuoteText() {
		quoteRepository.save(new Quote("Be here now."));
		quoteRepository.save(new Quote("Be here now."));

		assertThatThrownBy(() -> quoteRepository.flush())
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}