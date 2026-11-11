package com.jobtracker.offre;

import com.jobtracker.offre.OffreDtos.OffreExterne;
import com.jobtracker.offre.OffreDtos.OffresResponse;
import com.jobtracker.offre.OffreDtos.Recherche;
import com.jobtracker.offre.OffreDtos.ResultatSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Recherche d'offres d'emploi dans les API externes configurees. Par defaut, toutes les sources du pays sont
 * interrogees en parallele et leurs offres fusionnees ("Toutes les sources") ; on peut aussi n'en choisir qu'une.
 */
@Service
public class OffreService {

    /** Nom de l'onglet qui regroupe toutes les sources. */
    public static final String TOUTES = "Toutes les sources";
    private static final Logger LOG = LoggerFactory.getLogger(OffreService.class);
    private static final Comparator<OffreExterne> PLUS_RECENTES = Comparator.comparing(OffreExterne::datePublication,
            Comparator.nullsLast(Comparator.reverseOrder()));

    private final List<SourceOffres> sources;
    private final ExecutorService executeur = Executors.newVirtualThreadPerTaskExecutor();

    public OffreService(List<SourceOffres> sources) {
        this.sources = sources;
    }

    public OffresResponse rechercher(String nomSource, Recherche recherche) {
        List<SourceOffres> disponibles = sources.stream().filter(SourceOffres::configuree).toList();
        if (disponibles.isEmpty()) {
            throw new OffresIndisponiblesException("Aucune API d'offres configuree : definis LBA_API_KEY "
                    + "(https://api.apprentissage.beta.gouv.fr, offres d'alternance) ou JOOBLE_API_KEY "
                    + "(https://fr.jooble.org/api/about, tous contrats)");
        }
        String nomPays = Catalogue.pays(recherche.pays()).map(Catalogue.Pays::nom).orElse(recherche.pays());
        List<SourceOffres> duPays = disponibles.stream().filter(s -> s.couvre(recherche.pays())).toList();
        if (duPays.isEmpty()) {
            throw new OffresIndisponiblesException("Aucune source d'offres pour ce pays (" + nomPays + ") pour l'instant.");
        }
        List<SourceOffres> compatibles = duPays.stream().filter(s -> s.supporte(recherche.contrat())).toList();
        if (compatibles.isEmpty()) {
            throw new OffresIndisponiblesException("Aucune source ne propose ce type de contrat pour " + nomPays
                    + " : essaie \"Tous les contrats\".");
        }
        List<String> onglets = new ArrayList<>();
        if (compatibles.size() > 1) {
            onglets.add(TOUTES);
        }
        compatibles.forEach(s -> onglets.add(s.nom()));

        SourceOffres choisie = compatibles.stream().filter(s -> s.nom().equalsIgnoreCase(nomSource)).findFirst()
                .orElse(null);
        if (choisie == null && compatibles.size() > 1) {
            return toutes(compatibles, recherche, onglets);
        }
        SourceOffres source = choisie != null ? choisie : compatibles.getFirst();
        ResultatSource resultat = interroger(source, recherche);
        List<OffreExterne> offres = recherche.parDate()
                ? resultat.offres().stream().sorted(PLUS_RECENTES).toList() : resultat.offres();
        long vus = (long) (recherche.page() + 1) * SourceOffres.TAILLE_PAGE;
        return new OffresResponse(onglets, source.nom(), offres, resultat.total(), recherche.page(),
                resultat.offres().size() == SourceOffres.TAILLE_PAGE && vus < resultat.total(),
                source.remarque(recherche));
    }

    /** Interroge toutes les sources en parallele et fusionne leurs offres (sans doublon de lien). */
    private OffresResponse toutes(List<SourceOffres> compatibles, Recherche recherche, List<String> onglets) {
        Map<SourceOffres, Future<ResultatSource>> appels = new LinkedHashMap<>();
        compatibles.forEach(s -> appels.put(s, executeur.submit(() -> interroger(s, recherche))));
        List<List<OffreExterne>> pages = new ArrayList<>();
        List<String> echecs = new ArrayList<>();
        long total = 0;
        boolean suite = false;
        long vus = (long) (recherche.page() + 1) * SourceOffres.TAILLE_PAGE;
        RuntimeException derniere = null;
        for (Map.Entry<SourceOffres, Future<ResultatSource>> appel : appels.entrySet()) {
            try {
                ResultatSource r = appel.getValue().get(25, TimeUnit.SECONDS);
                pages.add(r.offres());
                total += r.total();
                suite |= r.offres().size() == SourceOffres.TAILLE_PAGE && vus < r.total();
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                LOG.warn("Source {} indisponible : {}", appel.getKey().nom(), cause.getMessage());
                echecs.add(appel.getKey().nom());
                derniere = cause instanceof RuntimeException re ? re : new OffresIndisponiblesException(cause.getMessage());
            }
        }
        if (pages.isEmpty() && derniere != null) {
            throw derniere;
        }
        List<OffreExterne> fusion = recherche.parDate() ? fusionnerParDate(pages) : alterner(pages);
        String remarque = "Offres réunies de " + String.join(", ", compatibles.stream().map(SourceOffres::nom)
                .filter(n -> !echecs.contains(n)).toList()) + "."
                + (echecs.isEmpty() ? "" : " Indisponible pour le moment : " + String.join(", ", echecs) + ".");
        return new OffresResponse(onglets, TOUTES, fusion, total, recherche.page(), suite, remarque);
    }

    /** Pertinence : on alterne les sources pour ne pas noyer l'une sous l'autre. */
    static List<OffreExterne> alterner(List<List<OffreExterne>> pages) {
        List<OffreExterne> resultat = new ArrayList<>();
        java.util.Set<String> liens = new java.util.HashSet<>();
        int max = pages.stream().mapToInt(List::size).max().orElse(0);
        for (int i = 0; i < max; i++) {
            for (List<OffreExterne> page : pages) {
                if (i < page.size() && (page.get(i).url() == null || liens.add(page.get(i).url()))) {
                    resultat.add(page.get(i));
                }
            }
        }
        return resultat;
    }

    static List<OffreExterne> fusionnerParDate(List<List<OffreExterne>> pages) {
        java.util.Set<String> liens = new java.util.HashSet<>();
        return pages.stream().flatMap(List::stream)
                .filter(o -> o.url() == null || liens.add(o.url()))
                .sorted(PLUS_RECENTES)
                .toList();
    }

    private static ResultatSource interroger(SourceOffres source, Recherche recherche) {
        try {
            return source.rechercher(recherche);
        } catch (RestClientResponseException e) {
            // Certaines API renvoient une page HTML complete en cas d'erreur : on n'affiche que le code
            String corps = e.getResponseBodyAsString();
            String detail = corps.isBlank() || corps.stripLeading().startsWith("<") || corps.length() > 300
                    ? "" : " : " + corps.strip();
            throw new OffresIndisponiblesException("L'API " + source.nom() + " a refuse la recherche (erreur "
                    + e.getStatusCode().value() + ")" + detail + source.aideErreur(e.getStatusCode().value()));
        } catch (RestClientException e) {
            throw new OffresIndisponiblesException("L'API " + source.nom() + " ne repond pas : " + e.getMessage());
        }
    }
}
