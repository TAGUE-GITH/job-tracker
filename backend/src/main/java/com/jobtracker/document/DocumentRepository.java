package com.jobtracker.document;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByUserIdOrderByDateAjoutDesc(Long userId);

    Optional<Document> findByIdAndUserId(Long id, Long userId);
}
