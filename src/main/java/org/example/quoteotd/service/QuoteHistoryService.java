package org.example.quoteotd.service;

import java.util.List;
import java.util.UUID;
import org.example.quoteotd.dto.QuoteHistoryResponse;
import org.example.quoteotd.exception.ViewerNotFoundException;
import org.example.quoteotd.model.QuoteHistory;
import org.example.quoteotd.repository.QuoteHistoryRepository;
import org.example.quoteotd.repository.ViewerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuoteHistoryService {

	private final QuoteHistoryRepository quoteHistoryRepository;
	private final ViewerRepository viewerRepository;

	public QuoteHistoryService(QuoteHistoryRepository quoteHistoryRepository, ViewerRepository viewerRepository) {
		this.quoteHistoryRepository = quoteHistoryRepository;
		this.viewerRepository = viewerRepository;
	}

	@Transactional(readOnly = true)
	public List<QuoteHistoryResponse> findHistory(UUID viewerId) {
		if (!viewerRepository.existsById(viewerId)) {
			throw new ViewerNotFoundException(viewerId);
		}
		return quoteHistoryRepository.findAllByIdViewerIdOrderBySequenceDesc(viewerId).stream()
				.map(QuoteHistoryService::toResponse)
				.toList();
	}

	private static QuoteHistoryResponse toResponse(QuoteHistory row) {
		return new QuoteHistoryResponse(row.getId().viewerId(), row.getId().quoteId(),
				row.getId().cycle(), row.getSequence(), row.getShownAt());
	}
}
