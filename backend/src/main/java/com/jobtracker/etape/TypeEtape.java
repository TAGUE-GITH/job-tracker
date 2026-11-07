package com.jobtracker.etape;

import com.jobtracker.candidature.StatutCandidature;

public enum TypeEtape {
    CANDIDATURE_ENVOYEE(StatutCandidature.EN_ATTENTE),
    ECHANGE_RH(StatutCandidature.EN_COURS),
    TEST_TECHNIQUE(StatutCandidature.EN_COURS),
    ENTRETIEN_TECHNIQUE(StatutCandidature.EN_COURS),
    ENTRETIEN_MANAGER(StatutCandidature.EN_COURS),
    OFFRE(StatutCandidature.OFFRE_RECUE),
    REFUS(StatutCandidature.REFUSEE),
    /** Relance envoyee par le candidat : ne change pas le statut. */
    RELANCE(null);

    /** Statut que prend la candidature quand cette etape est la plus recente (null : inchange). */
    private final StatutCandidature statutResultant;

    TypeEtape(StatutCandidature statutResultant) {
        this.statutResultant = statutResultant;
    }

    public StatutCandidature statutResultant() {
        return statutResultant;
    }

    public boolean estEntretien() {
        return this == ENTRETIEN_TECHNIQUE || this == ENTRETIEN_MANAGER || this == TEST_TECHNIQUE;
    }
}
