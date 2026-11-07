package com.jobtracker.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

// "user" est un mot reserve en PostgreSQL, d'ou le nom de table "users"
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    // Coordonnees affichees en haut des lettres de motivation (facultatives)
    @Column(length = 30)
    private String telephone;

    @Column(length = 300)
    private String adresse;

    @Column(length = 100)
    private String ville;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    protected User() {
    }

    public User(String nom, String email, String password) {
        this.nom = nom;
        this.email = email;
        this.password = password;
        this.dateCreation = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getEmail() {
        return email;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public String getTelephone() {
        return telephone;
    }

    public String getAdresse() {
        return adresse;
    }

    public String getVille() {
        return ville;
    }

    public void modifierProfil(String nom, String telephone, String adresse, String ville) {
        this.nom = nom;
        this.telephone = telephone;
        this.adresse = adresse;
        this.ville = ville;
    }

    public void changerMotDePasse(String hash) {
        this.password = hash;
    }

    public String getPassword() {
        return password;
    }
}
