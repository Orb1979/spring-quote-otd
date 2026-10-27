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

	/*
	JPA derived method:
	Optional<QuoteHistory> findFirstByIdViewerIdOrderBySequenceDesc(UUID viewerId);

	Would be the same as this raw SQL query:

	@Query(value = """
    SELECT *
    FROM quote_history
    WHERE viewer_id = :viewerId
    ORDER BY sequence DESC
    LIMIT 1
    """, nativeQuery = true)
	Optional<QuoteHistory> findFirstByIdViewerIdOrderBySequenceDesc(@Param("viewerId") UUID viewerId);

  Or this JPQL version:

	@Query("""
    SELECT h
    FROM QuoteHistory h
    WHERE h.id.viewerId = :viewerId
    ORDER BY h.sequence DESC
    """)
	List<QuoteHistory> findHistoryForViewer(
			@Param("viewerId") UUID viewerId,
			Pageable pageable);

	it needs a Pageable to limit the results, JPQL is designed to work across different databases
	and SQL’s LIMIT 1 isn’t part of the JPQL language.
  A derived method can still limit results because Spring recognizes First in its method name and tells
  JPA to return at most one result. That limit is applied separately from the query text.
  */
