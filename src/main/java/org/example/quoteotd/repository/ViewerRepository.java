package org.example.quoteotd.repository;

import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.example.quoteotd.model.Viewer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ViewerRepository extends JpaRepository<Viewer, UUID> {

	/**
	 * Loads the viewer under a pessimistic write lock, serializing concurrent
	 * next-quote requests for the same viewer.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select v from Viewer v where v.id = :id")
	Optional<Viewer> lockById(@Param("id") UUID id);
}
