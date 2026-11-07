package com.jobtracker.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    long countByEmailEndingWith(String suffixe);

    List<User> findByEmailEndingWithAndDateCreationBefore(String suffixe, LocalDateTime limite);

    boolean existsByEmail(String email);
}
