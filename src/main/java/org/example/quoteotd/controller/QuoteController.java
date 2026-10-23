package org.example.quoteotd.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.example.quoteotd.dto.QuoteRequest;
import org.example.quoteotd.dto.QuoteResponse;
import org.example.quoteotd.service.QuoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quotes")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @GetMapping
    public List<QuoteResponse> findAll() {
        return quoteService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuoteResponse> findById(@PathVariable UUID id) {
        return quoteService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<QuoteResponse> create(@RequestBody QuoteRequest request) {
        QuoteResponse created = quoteService.create(request);
        return ResponseEntity.created(URI.create("/api/quotes/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuoteResponse> update(@PathVariable UUID id, @RequestBody QuoteRequest request) {
        return quoteService.update(id, request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
        if (!quoteService.deleteById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
