package com.jobtracker.offre;

import com.jobtracker.offre.RechercheService.Intention;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Comprehension de secours, sans IA : abreviations courantes, intitules equivalents et traductions des metiers
 * les plus recherches. L'IA fait bien mieux ; ceci evite seulement une recherche "betement" litterale.
 */
final class Dictionnaire {

    private Dictionnaire() {
    }

    /** Metier : mots qui le reperent dans la requete, intitule francais, anglais, equivalents, competences. */
    private record Metier(List<String> reperes, String fr, String en, List<String> equivalents,
            List<String> competences, String domaine) {
    }

    private static final List<Metier> METIERS = List.of(
            new Metier(List.of("full stack"), "Développeur full stack", "Full stack developer",
                    List.of("Développeur web", "Software engineer", "Développeur fullstack JavaScript"),
                    List.of("JavaScript", "React", "Node.js", "API REST", "SQL"), "INFORMATIQUE"),
            new Metier(List.of("frontend", "front"), "Développeur front-end", "Frontend developer",
                    List.of("Développeur web", "Intégrateur web", "Développeur React"),
                    List.of("JavaScript", "React", "TypeScript", "HTML", "CSS"), "INFORMATIQUE"),
            new Metier(List.of("backend", "back"), "Développeur back-end", "Backend developer",
                    List.of("Développeur Java", "Développeur Python", "Software engineer"),
                    List.of("API REST", "SQL", "Java", "Python", "Docker"), "INFORMATIQUE"),
            new Metier(List.of("devops"), "Ingénieur DevOps", "DevOps engineer",
                    List.of("Ingénieur cloud", "Site reliability engineer", "Administrateur systèmes"),
                    List.of("Docker", "Kubernetes", "CI/CD", "Linux", "AWS"), "INFORMATIQUE"),
            new Metier(List.of("data analyst", "analyste de donnees", "analyste donnees"), "Data analyst", "Data analyst",
                    List.of("Analyste de données", "Business analyst", "Analyste BI"),
                    List.of("SQL", "Python", "Power BI", "Excel", "Tableau"), "DATA"),
            new Metier(List.of("data scientist"), "Data scientist", "Data scientist",
                    List.of("Machine learning engineer", "Ingénieur IA", "Data analyst"),
                    List.of("Python", "Machine learning", "SQL", "Statistiques"), "DATA"),
            new Metier(List.of("developpeur", "dev", "developer", "programmeur"), "Développeur", "Software developer",
                    List.of("Développeur web", "Ingénieur logiciel", "Software engineer"),
                    List.of("Git", "SQL", "API"), "INFORMATIQUE"),
            new Metier(List.of("compta", "comptable", "comptabilite"), "Comptable", "Accountant",
                    List.of("Assistant comptable", "Collaborateur comptable", "Gestionnaire comptable"),
                    List.of("Comptabilité", "Sage", "Excel", "Fiscalité"), "FINANCE"),
            new Metier(List.of("rh", "ressources humaines"), "Chargé de ressources humaines", "HR officer",
                    List.of("Gestionnaire RH", "Chargé de recrutement", "Assistant RH"),
                    List.of("Recrutement", "Paie", "Droit du travail"), "RH"),
            new Metier(List.of("commercial", "vente", "vendeur", "business developer"), "Commercial", "Sales representative",
                    List.of("Business developer", "Chargé d'affaires", "Conseiller commercial"),
                    List.of("Prospection", "Négociation", "CRM"), "COMMERCE"),
            new Metier(List.of("marketing", "communication"), "Chargé de marketing", "Marketing officer",
                    List.of("Chargé de communication", "Marketing digital", "Community manager"),
                    List.of("SEO", "Réseaux sociaux", "Google Analytics"), "MARKETING"),
            new Metier(List.of("infirmier", "infirmiere"), "Infirmier", "Nurse",
                    List.of("Infirmière", "IDE", "Infirmier diplômé d'État"),
                    List.of("Soins", "Hôpital"), "SANTE"),
            new Metier(List.of("logistique", "logisticien", "magasinier", "cariste"), "Logisticien", "Logistics operator",
                    List.of("Agent logistique", "Magasinier", "Préparateur de commandes"),
                    List.of("Gestion des stocks", "CACES"), "LOGISTIQUE"));

    static Intention comprendre(String requete) {
        String n = " " + RechercheService.normaliser(requete) + " ";
        String contrat = n.contains(" alternance ") || n.contains(" apprentissage ") ? "ALTERNANCE"
                : n.contains(" stage ") || n.contains(" stagiaire ") ? "STAGE"
                : n.contains(" cdi ") ? "CDI" : n.contains(" cdd ") ? "CDD" : "";
        // Le contrat ne fait pas partie de l'intitule cherche
        String sansContrat = (" " + n.replaceAll("\\b(alternance|apprentissage|stage|stagiaire|cdi|cdd|en|h f|f h)\\b", " ")
                .replaceAll("\\s+", " ").strip() + " ").replace(" dev ", " developpeur ").strip();
        for (Metier m : METIERS) {
            if (m.reperes().stream().anyMatch(r -> (" " + sansContrat + " ").contains(" " + r + " "))) {
                Set<String> equivalents = new LinkedHashSet<>();
                // Garde les precisions de l'utilisateur (ex. "developpeur java" -> "Developpeur java")
                String precisions = " " + sansContrat + " ";
                for (String r : m.reperes()) {
                    precisions = precisions.replace(" " + r + " ", " ");
                }
                precisions = (" " + precisions.strip() + " ").replaceAll(" (developpeur|developer) ", " ").strip();
                String intitule = precisions.isEmpty() ? m.fr() : m.fr() + " " + precisions;
                equivalents.add(m.fr());
                equivalents.addAll(m.equivalents());
                equivalents.remove(intitule);
                return new Intention(intitule, m.en(),
                        new ArrayList<>(equivalents), m.competences(), m.domaine(), contrat, "");
            }
        }
        String intitule = majuscule(sansContrat.isBlank() ? requete.strip() : sansContrat);
        return new Intention(intitule, "", List.of(), List.of(), "", contrat, "");
    }

    private static String majuscule(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
