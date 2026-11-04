package com.jobtracker.etape;

import com.jobtracker.candidature.Candidature;
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
public class Etape {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeEtape type;

    @Column(name = "date_etape", nullable = false)
    private LocalDate date;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidature_id")
    private Candidature candidature;

    protected Etape() {
    }

    public Etape(TypeEtape type, LocalDate date, String notes) {
        this.type = type;
        this.date = date;
        this.notes = notes;
    }

    public void modifier(TypeEtape type, LocalDate date, String notes) {
        this.type = type;
        this.date = date;
        this.notes = notes;
    }

    public void setCandidature(Candidature candidature) {
        this.candidature = candidature;
    }

    public Long getId() {
        return id;
    }

    public TypeEtape getType() {
        return type;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getNotes() {
        return notes;
    }

    public Candidature getCandidature() {
        return candidature;
    }
}
