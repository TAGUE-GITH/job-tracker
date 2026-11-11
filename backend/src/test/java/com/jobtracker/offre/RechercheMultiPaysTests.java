package com.jobtracker.offre;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jobtracker.offre.OffreDtos.OffresResponse;
import com.jobtracker.offre.OffreDtos.Recherche;
import com.jobtracker.offre.OffreDtos.ResultatSource;
import java.util.List;
import org.junit.jupiter.api.Test;

class RechercheMultiPaysTests {

    @Test
    void joobleAccepteUneCleParPays() {
        JoobleSource jooble = new JoobleSource("cle-fr", "fr.jooble.org", "be=cle-be, ca = cle-ca ;gb=cle-uk");
        assertTrue(jooble.couvre("FR"));
        assertTrue(jooble.couvre("be"));
        assertTrue(jooble.couvre("CA"));
        assertTrue(jooble.couvre("GB"));
        assertFalse(jooble.couvre("DE"));
        assertEquals("GB", JoobleSource.paysDuHote("uk.jooble.org"));
        assertEquals("US", JoobleSource.paysDuHote("jooble.org"));
        assertFalse(new JoobleSource("", "fr.jooble.org", "").configuree());
    }

    @Test
    void leDomaineRemplaceLesMotsClesVides() {
        Recherche r = new Recherche("", "", "", 0, "FR", Catalogue.Domaine.FINANCE);
        assertEquals("comptabilité", r.requeteFr());
        assertEquals("finance", r.requeteEn());
        assertEquals("audit", new Recherche("audit", "", "", 0, "FR", Catalogue.Domaine.FINANCE).requeteFr());
        assertTrue(Catalogue.Domaine.INFORMATIQUE.reconnu("senior software engineer (m/w/d)"));
        assertFalse(Catalogue.Domaine.SANTE.reconnu("senior software engineer"));
        assertEquals("Cameroun", Catalogue.pays("cm").orElseThrow().nom());
    }

    /** Source factice : un nom, les pays couverts et les contrats acceptes. */
    private static SourceOffres source(String nom, List<String> pays, boolean alternance) {
        return new SourceOffres() {
            public String nom() {
                return nom;
            }

            public boolean configuree() {
                return true;
            }

            public boolean couvre(String p) {
                return pays.contains(p);
            }

            public boolean supporte(String contrat) {
                return alternance || !"ALTERNANCE".equals(contrat);
            }

            public ResultatSource rechercher(Recherche r) {
                return new ResultatSource(List.of(), 0);
            }
        };
    }

    private static OffreDtos.OffreExterne offre(String id, String url, java.time.LocalDate date) {
        return new OffreDtos.OffreExterne(id, "s", id, null, null, null, null, null, url, date);
    }

    @Test
    void laFusionTrieParDateEtRetireLesDoublons() {
        var a = List.of(offre("a1", "u1", java.time.LocalDate.of(2026, 11, 1)), offre("a2", "u2", null));
        var b = List.of(offre("b1", "u3", java.time.LocalDate.of(2026, 11, 9)), offre("b2", "u1", java.time.LocalDate.of(2026, 11, 1)));
        assertEquals(List.of("b1", "a1", "a2"), OffreService.fusionnerParDate(List.of(a, b)).stream()
                .map(OffreDtos.OffreExterne::id).toList());
        assertEquals(List.of("a1", "b1", "a2"), OffreService.alterner(List.of(a, b)).stream()
                .map(OffreDtos.OffreExterne::id).toList());
    }

    @Test
    void foremTraduitLesCriteresEnFiltre() {
        String f = ForemSource.filtre(new Recherche("aide comptable", "Liège", "CDI", 0, "BE", null));
        assertTrue(f.contains("search(titreoffre, \"comptable\")"), f);
        assertTrue(f.contains("search(lieuxtravaillocalite, \"Liège\")"), f);
        assertTrue(f.contains("indéterminée"), f);
        assertEquals("Adecco Personnel Services", ForemSource.capitaliser("ADECCO PERSONNEL SERVICES"));
    }

    @Test
    void lesSourcesProposeesDependentDuPays() {
        OffreService service = new OffreService(List.of(
                source("LBA", List.of("FR"), true),
                source("Monde", List.of("FR", "CA", "DE"), false),
                source("Allemagne", List.of("DE"), true)));

        OffresResponse france = service.rechercher("", new Recherche("", "", "", 0, "FR", null));
        assertEquals(List.of(OffreService.TOUTES, "LBA", "Monde"), france.sources());
        assertEquals(OffreService.TOUTES, france.source());
        OffresResponse canada = service.rechercher("", new Recherche("", "", "", 0, "CA", null));
        assertEquals(List.of("Monde"), canada.sources());
        assertNull(canada.remarque());
        // L'alternance au Canada : aucune source ne la propose
        assertThrows(OffresIndisponiblesException.class,
                () -> service.rechercher("", new Recherche("", "", "ALTERNANCE", 0, "CA", null)));
        assertThrows(OffresIndisponiblesException.class,
                () -> service.rechercher("", new Recherche("", "", "", 0, "SN", null)));
    }
}
