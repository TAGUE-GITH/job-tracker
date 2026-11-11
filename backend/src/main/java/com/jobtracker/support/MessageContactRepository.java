package com.jobtracker.support;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageContactRepository extends JpaRepository<MessageContact, Long> {

    /** Non traites d'abord, puis du plus recent au plus ancien. */
    List<MessageContact> findAllByOrderByTraiteAscDateCreationDesc();

    long countByTraiteFalse();

    long countByEmailIgnoreCaseAndDateCreationAfter(String email, LocalDateTime depuis);
}
