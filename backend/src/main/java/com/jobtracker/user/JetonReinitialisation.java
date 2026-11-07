package com.jobtracker.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Lien "mot de passe oublie" : a usage unique, valable 30 minutes. Seul le hash du jeton est stocke. */
@Entity
@Table(name = "jeton_reinitialisation")
public class JetonReinitialisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 64, unique = true)
    private String hash;

    @Column(nullable = false)
    private LocalDateTime expiration;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(nullable = false)
    private boolean utilise;

    protected JetonReinitialisation() {
    }

    public JetonReinitialisation(User user, String hash, LocalDateTime expiration) {
        this.user = user;
        this.hash = hash;
        this.expiration = expiration;
        this.dateCreation = LocalDateTime.now();
    }

    public boolean valide() {
        return !utilise && LocalDateTime.now().isBefore(expiration);
    }

    public void utiliser() {
        this.utilise = true;
    }

    public User getUser() {
        return user;
    }
}
