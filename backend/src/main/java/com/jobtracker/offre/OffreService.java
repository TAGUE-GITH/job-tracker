package com.jobtracker.offre;

import com.jobtracker.offre.OffreDtos.OffresResponse;
import com.jobtracker.offre.OffreDtos.Recherche;
import com.jobtracker.offre.OffreDtos.ResultatSource;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/** Recherche d'offres d'emploi dans les API externes configurees. */
@Service
public class OffreService {

    private final List<SourceOffres> sources;

    public OffreService(List<SourceOffres> sources) {
        this.sources = sources;
    }

    public OffresResponse rechercher(String nomSource, Recherche recherche) {
        List<SourceOffres> disponibles = sources.stream().filter(SourceOffres::configuree).toList();
        if (disponibles.isEmpty()) {
            throw new OffresIndisponiblesException("Aucune API d'offres configuree : definis FT_CLIENT_ID et "
                    + "FT_CLIENT_SECRET (https://francetravail.io) ou ADZUNA_APP_ID et ADZUNA_APP_KEY "
                    + "(https://developer.adzuna.com)");
        }
        SourceOffres source = disponibles.stream()
                .filter(s -> s.nom().equalsIgnoreCase(nomSource))
                .findFirst()
                .orElse(disponibles.getFirst());

        ResultatSource resultat;
        try {
            resultat = source.rechercher(recherche);
        } catch (RestClientException e) {
            throw new OffresIndisponiblesException("L'API " + source.nom() + " ne repond pas : " + e.getMessage());
        }
        long vus = (long) (recherche.page() + 1) * SourceOffres.TAILLE_PAGE;
        return new OffresResponse(
                disponibles.stream().map(SourceOffres::nom).toList(),
                source.nom(),
                resultat.offres(),
                resultat.total(),
                recherche.page(),
                resultat.offres().size() == SourceOffres.TAILLE_PAGE && vus < resultat.total());
    }
}
