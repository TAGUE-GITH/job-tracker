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

    /** Incrementee pour deconnecter toutes les sessions (voir le claim "ver" du JWT). */
    @Column(name = "version_jeton", nullable = false)
    private int versionJeton;

    /** Nomme administrateur depuis la page Administration. */
    @Column(nullable = false)
    private boolean administrateur;

    /** Profil extrait du CV par l'IA (JSON), pour recommander des offres ; null tant qu'aucun CV n'est analyse. */
    @Column(name = "profil_cv", length = 8000)
    private String profilCv;

    @Column(name = "profil_cv_document_id")
    private Long profilCvDocumentId;

    /** Date d'acceptation des conditions d'utilisation (null : compte cree avant leur mise en place). */
    @Column(name = "conditions_acceptees_le")
    private LocalDateTime conditionsAccepteesLe;

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

    public int getVersionJeton() {
        return versionJeton;
    }

    /** Invalide tous les jetons de connexion deja emis pour ce compte. */
    public void revoquerJetons() {
        this.versionJeton++;
    }

    public String getProfilCv() {
        return profilCv;
    }

    public Long getProfilCvDocumentId() {
        return profilCvDocumentId;
    }

    public void definirProfilCv(String profilCv, Long documentId) {
        this.profilCv = profilCv;
        this.profilCvDocumentId = documentId;
    }

    public LocalDateTime getConditionsAccepteesLe() {
        return conditionsAccepteesLe;
    }

    public void accepterConditions() {
        this.conditionsAccepteesLe = LocalDateTime.now();
    }

    public boolean isAdministrateur() {
        return administrateur;
    }

    public void definirAdministrateur(boolean administrateur) {
        this.administrateur = administrateur;
    }

    public void changerMotDePasse(String hash) {
        this.password = hash;
    }

    public String getPassword() {
        return password;
    }
}
