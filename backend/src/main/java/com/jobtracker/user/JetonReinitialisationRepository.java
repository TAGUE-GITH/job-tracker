package com.jobtracker.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface JetonReinitialisationRepository extends JpaRepository<JetonReinitialisation, Long> {

    Optional<JetonReinitialisation> findByHash(String hash);

    long countByUserIdAndDateCreationAfter(Long userId, LocalDateTime depuis);

    List<JetonReinitialisation> findByUserIdAndUtiliseFalse(Long userId);

    @Modifying
    @Query("delete from JetonReinitialisation j where j.user.id = :userId")
    void supprimerPourUtilisateur(Long userId);
}
