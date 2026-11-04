package com.jobtracker.candidature;

import com.jobtracker.document.Document;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CandidatureRepository extends JpaRepository<Candidature, Long> {

    @EntityGraph(attributePaths = {"etapes", "cv", "lettre"})
    List<Candidature> findByUserIdOrderByDateCandidatureDescIdDesc(Long userId);

    @EntityGraph(attributePaths = {"etapes", "cv", "lettre"})
    Optional<Candidature> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("update Candidature c set c.cv = null where c.cv = :doc")
    void detacherCv(Document doc);

    @Modifying
    @Query("update Candidature c set c.lettre = null where c.lettre = :doc")
    void detacherLettre(Document doc);
}
