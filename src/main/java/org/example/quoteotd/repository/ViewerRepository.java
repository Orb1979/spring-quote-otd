package org.example.quoteotd.repository;

import java.util.UUID;
import org.example.quoteotd.model.Viewer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ViewerRepository extends JpaRepository<Viewer, UUID> {
}
