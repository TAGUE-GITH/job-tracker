package com.jobtracker.support;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Message envoye depuis la page Contact ; userId est vide pour un visiteur non connecte. */
@Entity
@Table(name = "message_contact")
public class MessageContact {

    public enum Categorie { PROBLEME, QUESTION, SUGGESTION, AUTRE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Categorie categorie;

    @Column(nullable = false, length = 150)
    private String sujet;

    @Column(nullable = false, length = 5000)
    private String message;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(nullable = false)
    private boolean traite;

    protected MessageContact() {
    }

    public MessageContact(Long userId, String nom, String email, Categorie categorie, String sujet, String message) {
        this.userId = userId;
        this.nom = nom;
        this.email = email;
        this.categorie = categorie;
        this.sujet = sujet;
        this.message = message;
        this.dateCreation = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getNom() {
        return nom;
    }

    public String getEmail() {
        return email;
    }

    public Categorie getCategorie() {
        return categorie;
    }

    public String getSujet() {
        return sujet;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public boolean isTraite() {
        return traite;
    }

    public void marquerTraite(boolean traite) {
        this.traite = traite;
    }
}
