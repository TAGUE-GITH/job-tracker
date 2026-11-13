package com.jobtracker.document;

import com.jobtracker.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;

@Entity
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomFichier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeDocument type;

    private String contentType;

    /** Vrai pour un document cree par Postulyn (CV adapte, lettre), faux pour un fichier importe. */
    @Column(nullable = false)
    private boolean genere;

    @Column(nullable = false)
    private LocalDate dateAjout;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    protected Document() {
    }

    public Document(String nomFichier, TypeDocument type, String contentType, User user) {
        this.nomFichier = nomFichier;
        this.type = type;
        this.contentType = contentType;
        this.user = user;
        this.dateAjout = LocalDate.now();
    }

    public Document genere() {
        this.genere = true;
        return this;
    }

    public boolean isGenere() {
        return genere;
    }

    public Long getId() {
        return id;
    }

    public String getNomFichier() {
        return nomFichier;
    }

    public TypeDocument getType() {
        return type;
    }

    public String getContentType() {
        return contentType;
    }

    public LocalDate getDateAjout() {
        return dateAjout;
    }

    public User getUser() {
        return user;
    }
}
