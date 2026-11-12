package com.jobtracker.offre;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jobtracker.offre.OffreDtos.OffreExterne;
import com.jobtracker.offre.RechercheService.Intention;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class RechercheIntelligenteTests {

    private static OffreExterne offre(String titre, String description, LocalDate date) {
        return new OffreExterne(titre, "s", titre, "ACME", "Paris", null, null, description, "https://o/" + titre, date);
    }

    @Test
    void leDictionnaireComprendLesAbreviationsEtLeContrat() {
        Intention i = Dictionnaire.comprendre("dev full-stack en alternance");
        assertEquals("Développeur full stack", i.intitule());
        assertEquals("Full stack developer", i.intituleEn());
        assertEquals("ALTERNANCE", i.contrat());
        assertTrue(i.formulations().contains("Développeur web"), i.formulations().toString());

        Intention compta = Dictionnaire.comprendre("compta CDI");
        assertEquals("Comptable", compta.intitule());
        assertEquals("CDI", compta.contrat());

        // Metier inconnu : la recherche reste litterale, sans planter
        assertEquals("Apiculteur", Dictionnaire.comprendre("apiculteur").intitule());
    }

    @Test
    void seulesLesOffresPertinentesSontGardeesEtClassees() {
        Intention i = Dictionnaire.comprendre("développeur full stack");
        LocalDate aujourdhui = LocalDate.now();
        List<OffreExterne> offres = List.of(
                offre("Technicien informatique (H/F)", "Maintenance du parc et support utilisateurs", aujourdhui),
                offre("Fullstack Developer React / Node", "API REST, JavaScript, SQL", aujourdhui),
                offre("Développeur web", "Vous développerez en JavaScript et React", aujourdhui),
                offre("Chef de rayon", "Gestion d'équipe en magasin", aujourdhui));
        List<RechercheService.OffreTrouvee> gardees = RechercheService.filtrer(offres, i, false, 0);
        List<String> titres = gardees.stream().map(o -> o.offre().titre()).toList();
        assertTrue(titres.contains("Fullstack Developer React / Node"), titres.toString());
        assertTrue(titres.contains("Développeur web"), titres.toString());
        assertTrue(!titres.contains("Chef de rayon") && !titres.contains("Technicien informatique (H/F)"), titres.toString());
        int fullstack = RechercheService.pertinence(offres.get(1), i);
        int web = RechercheService.pertinence(offres.get(2), i);
        assertTrue(fullstack > web, fullstack + " / " + web);
    }

    @Test
    void leFiltreDeDateEtLesDoublonsSontAppliques() {
        Intention i = Dictionnaire.comprendre("comptable");
        List<OffreExterne> offres = List.of(
                offre("Comptable", "", LocalDate.now().minusDays(2)),
                offre("Comptable", "", LocalDate.now().minusDays(2)),
                offre("Comptable fournisseurs", "", LocalDate.now().minusDays(20)),
                offre("Assistant comptable", "", null));
        assertEquals(1, RechercheService.filtrer(offres, i, false, 7).size());
        assertEquals(3, RechercheService.filtrer(offres, i, false, 0).size());
    }
}
