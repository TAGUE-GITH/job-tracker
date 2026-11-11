package com.jobtracker.document;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByUserIdOrderByDateAjoutDesc(Long userId);

    Optional<Document> findByIdAndUserId(Long id, Long userId);

    @org.springframework.data.jpa.repository.Query("select count(d) from Document d where d.user.email not like :exclus")
    long compterHors(String exclus);

    /** Nombre de documents par utilisateur : lignes [userId, nombre]. */
    @org.springframework.data.jpa.repository.Query("select d.user.id, count(d) from Document d group by d.user.id")
    List<Object[]> compterParUtilisateur();
}
