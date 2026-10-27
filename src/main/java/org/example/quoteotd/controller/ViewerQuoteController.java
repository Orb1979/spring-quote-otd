package org.example.quoteotd.controller;

import java.util.List;
import java.util.UUID;
import org.example.quoteotd.dto.QuoteHistoryResponse;
import org.example.quoteotd.dto.QuoteResponse;
import org.example.quoteotd.service.NextQuoteService;
import org.example.quoteotd.service.QuoteHistoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/viewers/{viewerId}")
public class ViewerQuoteController {

	private final NextQuoteService nextQuoteService;
	private final QuoteHistoryService quoteHistoryService;

	public ViewerQuoteController(NextQuoteService nextQuoteService, QuoteHistoryService quoteHistoryService) {
		this.nextQuoteService = nextQuoteService;
		this.quoteHistoryService = quoteHistoryService;
	}

	@PostMapping("/quotes/next")
	public QuoteResponse nextQuote(@PathVariable UUID viewerId) {
		return nextQuoteService.nextQuote(viewerId);
	}

	@GetMapping("/history")
	public List<QuoteHistoryResponse> history(@PathVariable UUID viewerId) {
		return quoteHistoryService.findHistory(viewerId);
	}
}
