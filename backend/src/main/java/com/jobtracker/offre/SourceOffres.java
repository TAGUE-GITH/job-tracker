package com.jobtracker.offre;

import com.jobtracker.offre.OffreDtos.Recherche;
import com.jobtracker.offre.OffreDtos.ResultatSource;

/** Une API d'offres d'emploi (France Travail, Adzuna...). */
public interface SourceOffres {

    int TAILLE_PAGE = 20;

    /** Nom affiche dans l'application, sert aussi d'identifiant. */
    String nom();

    /** Vrai si les cles de l'API sont renseignees. */
    boolean configuree();

    ResultatSource rechercher(Recherche recherche);

    static String sansHtml(String texte) {
        return texte == null ? null : texte.replaceAll("<[^>]+>", "").replace("&amp;", "&").strip();
    }
}
