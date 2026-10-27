package org.example.quoteotd.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.quoteotd.model.QuoteHistory;
import org.example.quoteotd.model.QuoteHistoryId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuoteHistoryRepository extends JpaRepository<QuoteHistory, QuoteHistoryId> {

	/** The viewer's most recent row overall (highest sequence, across all cycles). */
	Optional<QuoteHistory> findFirstByIdViewerIdOrderBySequenceDesc(UUID viewerId);

	/** The viewer's full history, newest first (backed by idx_quote_history_recent). */
	List<QuoteHistory> findAllByIdViewerIdOrderBySequenceDesc(UUID viewerId);

	/** Quote ids the viewer has already seen in the given cycle. */
	@Query("select h.id.quoteId from QuoteHistory h where h.id.viewerId = :viewerId and h.id.cycle = :cycle")
	List<UUID> findQuoteIdsInCycle(@Param("viewerId") UUID viewerId, @Param("cycle") int cycle);

	/** The viewer's most recent quote ids, newest first, limited by the pageable's page size. */
	@Query("select h.id.quoteId from QuoteHistory h where h.id.viewerId = :viewerId order by h.sequence desc")
	List<UUID> findRecentQuoteIds(@Param("viewerId") UUID viewerId, Pageable pageable);

	/**
	 * Prunes rows the algorithm no longer needs: older cycles, but only up to
	 * {@code maxSequence} so the last K rows (the distance rule across a cycle
	 * boundary) survive. Never touches the current cycle.
	 */
	@Modifying
	@Query("delete from QuoteHistory h where h.id.viewerId = :viewerId"
			+ " and h.id.cycle < :currentCycle and h.sequence <= :maxSequence")
	int pruneHistory(@Param("viewerId") UUID viewerId, @Param("currentCycle") int currentCycle,
			@Param("maxSequence") int maxSequence);
}
