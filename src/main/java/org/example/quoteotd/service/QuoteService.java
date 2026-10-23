package org.example.quoteotd.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.quoteotd.dto.QuoteRequest;
import org.example.quoteotd.dto.QuoteResponse;
import org.example.quoteotd.model.Quote;
import org.example.quoteotd.repository.QuoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuoteService {

    private final QuoteRepository quoteRepository;

    public QuoteService(QuoteRepository quoteRepository) {
        this.quoteRepository = quoteRepository;
    }

    @Transactional(readOnly = true)
    public Optional<QuoteResponse> findById(UUID id) {
        return quoteRepository.findById(id).map(QuoteService::toResponse);
    }

    @Transactional(readOnly = true)
    public List<QuoteResponse> findAll() {
        return quoteRepository.findAll().stream().map(QuoteService::toResponse).toList();
    }

    @Transactional
    public QuoteResponse create(QuoteRequest request) {
        return toResponse(quoteRepository.save(new Quote(request.text())));
    }

    @Transactional
    public Optional<QuoteResponse> update(UUID id, QuoteRequest request) {
        return quoteRepository.findById(id)
                .map(quote -> {
                    quote.setText(request.text());
                    return quoteRepository.save(quote);
                })
                .map(QuoteService::toResponse);
    }

    @Transactional
    public boolean deleteById(UUID id) {
        if (!quoteRepository.existsById(id)) {
            return false;
        }
        quoteRepository.deleteById(id);
        return true;
    }

    private static QuoteResponse toResponse(Quote quote) {
        return new QuoteResponse(quote.getId(), quote.getText());
    }
}