package com.jobtracker.support;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvisRepository extends JpaRepository<Avis, Long> {

    List<Avis> findAllByOrderByDateCreationDesc();

    long countByUserIdAndDateCreationAfter(Long userId, LocalDateTime depuis);
}
