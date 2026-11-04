package com.jobtracker.candidature;

import com.jobtracker.document.Document;
import com.jobtracker.etape.Etape;
import com.jobtracker.user.User;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Entity
public class Candidature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomEntreprise;

    @Column(nullable = false)
    private String poste;

    @Column(length = 1000)
    private String lienOffre;

    @Column(columnDefinition = "TEXT")
    private String texteOffre;

    @Column(nullable = false)
    private LocalDate dateCandidature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutCandidature statut = StatutCandidature.EN_ATTENTE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_id")
    private Document cv;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lettre_id")
    private Document lettre;

    // Cote "plusieurs" de la relation : pas de colonne ici, c'est etape.candidature_id qui porte le lien
    @OneToMany(mappedBy = "candidature", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("date ASC, id ASC")
    private List<Etape> etapes = new ArrayList<>();

    protected Candidature() {
    }

    public Candidature(User user) {
        this.user = user;
    }

    public void ajouterEtape(Etape etape) {
        etape.setCandidature(this);
        etapes.add(etape);
        recalculerStatut();
    }

    public void retirerEtape(Etape etape) {
        etapes.remove(etape);
        recalculerStatut();
    }

    /** Le statut suit la derniere etape, sauf si la candidature a ete abandonnee manuellement. */
    public void recalculerStatut() {
        if (statut == StatutCandidature.ABANDONNEE) {
            return;
        }
        statut = etapes.stream()
                .max(Comparator.comparing(Etape::getDate).thenComparing(e -> e.getType().ordinal()))
                .map(e -> e.getType().statutResultant())
                .orElse(StatutCandidature.EN_ATTENTE);
    }

    public Long getId() {
        return id;
    }

    public String getNomEntreprise() {
        return nomEntreprise;
    }

    public void setNomEntreprise(String nomEntreprise) {
        this.nomEntreprise = nomEntreprise;
    }

    public String getPoste() {
        return poste;
    }

    public void setPoste(String poste) {
        this.poste = poste;
    }

    public String getLienOffre() {
        return lienOffre;
    }

    public void setLienOffre(String lienOffre) {
        this.lienOffre = lienOffre;
    }

    public String getTexteOffre() {
        return texteOffre;
    }

    public void setTexteOffre(String texteOffre) {
        this.texteOffre = texteOffre;
    }

    public LocalDate getDateCandidature() {
        return dateCandidature;
    }

    public void setDateCandidature(LocalDate dateCandidature) {
        this.dateCandidature = dateCandidature;
    }

    public StatutCandidature getStatut() {
        return statut;
    }

    public void setStatut(StatutCandidature statut) {
        this.statut = statut;
    }

    public User getUser() {
        return user;
    }

    public Document getCv() {
        return cv;
    }

    public void setCv(Document cv) {
        this.cv = cv;
    }

    public Document getLettre() {
        return lettre;
    }

    public void setLettre(Document lettre) {
        this.lettre = lettre;
    }

    public List<Etape> getEtapes() {
        return etapes;
    }
}
