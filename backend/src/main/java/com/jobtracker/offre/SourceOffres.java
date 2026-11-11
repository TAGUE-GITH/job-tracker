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

    /** Conseil ajoute au message quand l'API repond par une erreur HTTP (vide par defaut). */
    default String aideErreur(int statut) {
        return "";
    }

    /** Vrai si la source publie des offres pour ce pays (code ISO). Par defaut : la France. */
    default boolean couvre(String pays) {
        return "FR".equalsIgnoreCase(pays);
    }

    /** Precision affichee sous les resultats (ex. "offres en teletravail"), ou null. */
    default String remarque(Recherche recherche) {
        return null;
    }

    /** Faux si la source ne propose pas ce type de contrat (vide = tous). */
    default boolean supporte(String contrat) {
        return true;
    }

    /** Retire les espaces et les guillemets colles par erreur autour d'une cle d'API. */
    static String nettoyer(String valeur) {
        String v = valeur == null ? "" : valeur.strip();
        if (v.length() >= 2 && (v.startsWith("\"") && v.endsWith("\"") || v.startsWith("'") && v.endsWith("'"))) {
            v = v.substring(1, v.length() - 1).strip();
        }
        return v;
    }

    static String sansHtml(String texte) {
        return texte == null ? null : texte.replaceAll("<[^>]+>", "").replace("&amp;", "&").strip();
    }
}
