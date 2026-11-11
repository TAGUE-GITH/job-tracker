package com.jobtracker.user;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    long countByEmailEndingWith(String suffixe);

    List<User> findByEmailEndingWithAndDateCreationBefore(String suffixe, LocalDateTime limite);

    boolean existsByEmail(String email);

    // Statistiques d'administration : "exclus" est un motif LIKE (comptes de demonstration ignores)
    @Query("select count(u) from User u where u.email not like :exclus and u.dateCreation >= :depuis")
    long compterInscritsDepuis(LocalDateTime depuis, String exclus);

    @Query("select u.dateCreation from User u where u.email not like :exclus and u.dateCreation >= :depuis")
    List<LocalDateTime> datesInscription(LocalDateTime depuis, String exclus);
}
