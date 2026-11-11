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

    /** Lignes [statut, nombre] hors comptes dont l'email correspond au motif "exclus". */
    @Query("select c.statut, count(c) from Candidature c where c.user.email not like :exclus group by c.statut")
    List<Object[]> compterParStatut(String exclus);

    @Query("select c.dateCandidature from Candidature c where c.user.email not like :exclus and c.dateCandidature >= :depuis")
    List<java.time.LocalDate> datesCandidature(java.time.LocalDate depuis, String exclus);

    /** Nombre de candidatures par utilisateur : lignes [userId, nombre]. */
    @Query("select c.user.id, count(c) from Candidature c group by c.user.id")
    List<Object[]> compterParUtilisateur();

    @Modifying
    @Query("update Candidature c set c.cv = null where c.cv = :doc")
    void detacherCv(Document doc);

    @Modifying
    @Query("update Candidature c set c.lettre = null where c.lettre = :doc")
    void detacherLettre(Document doc);
}
