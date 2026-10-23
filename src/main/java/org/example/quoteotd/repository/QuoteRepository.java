package org.example.quoteotd.repository;

import java.util.UUID;
import org.example.quoteotd.model.Quote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteRepository extends JpaRepository<Quote, UUID> {
}
