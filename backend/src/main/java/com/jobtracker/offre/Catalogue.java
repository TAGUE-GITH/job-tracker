package com.jobtracker.offre;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Pays et domaines proposes dans la recherche d'offres. */
public final class Catalogue {

    private Catalogue() {
    }

    /**
     * code : ISO 3166-1 alpha-2 ; nomEn : nom anglais, utilise par les API internationales ;
     * dans : le nom precede de sa preposition ("en France", "au Canada", "aux Etats-Unis").
     */
    public record Pays(String code, String nom, String nomEn, String dans) {
    }

    public static final List<Pays> PAYS = List.of(
            new Pays("FR", "France", "France", "en France"),
            new Pays("BE", "Belgique", "Belgium", "en Belgique"),
            new Pays("CH", "Suisse", "Switzerland", "en Suisse"),
            new Pays("LU", "Luxembourg", "Luxembourg", "au Luxembourg"),
            new Pays("CA", "Canada", "Canada", "au Canada"),
            new Pays("DE", "Allemagne", "Germany", "en Allemagne"),
            new Pays("GB", "Royaume-Uni", "United Kingdom", "au Royaume-Uni"),
            new Pays("IE", "Irlande", "Ireland", "en Irlande"),
            new Pays("ES", "Espagne", "Spain", "en Espagne"),
            new Pays("PT", "Portugal", "Portugal", "au Portugal"),
            new Pays("IT", "Italie", "Italy", "en Italie"),
            new Pays("NL", "Pays-Bas", "Netherlands", "aux Pays-Bas"),
            new Pays("US", "États-Unis", "United States", "aux États-Unis"),
            new Pays("MA", "Maroc", "Morocco", "au Maroc"),
            new Pays("TN", "Tunisie", "Tunisia", "en Tunisie"),
            new Pays("SN", "Sénégal", "Senegal", "au Sénégal"),
            new Pays("CI", "Côte d'Ivoire", "Ivory Coast", "en Côte d'Ivoire"),
            new Pays("CM", "Cameroun", "Cameroon", "au Cameroun"));

    public static Optional<Pays> pays(String code) {
        return PAYS.stream().filter(p -> p.code().equalsIgnoreCase(code)).findFirst();
    }

    /**
     * Domaines d'activite. motFr / motEn : mot-cle envoye aux API quand l'utilisateur ne tape rien ;
     * reperes : mots (francais, anglais, allemand) qui reperent le domaine dans une offre.
     */
    public enum Domaine {
        INFORMATIQUE("Informatique et développement", "informatique", "software developer",
                "developpeur", "développeur", "informatique", "software", "developer", "engineer", "devops", "entwickler", "it "),
        DATA("Data et intelligence artificielle", "data", "data",
                "data", "donnees", "données", "machine learning", "analyst", "analytics", "daten"),
        MARKETING("Marketing et communication", "marketing", "marketing",
                "marketing", "communication", "seo", "content", "social media", "brand"),
        COMMERCE("Commerce et vente", "commercial", "sales",
                "commercial", "vente", "vendeur", "sales", "account", "business development", "vertrieb", "verkauf"),
        FINANCE("Finance et comptabilité", "comptabilité", "finance",
                "comptab", "finance", "financier", "audit", "accountant", "accounting", "buchhalt", "controlling"),
        RH("Ressources humaines", "ressources humaines", "human resources",
                "ressources humaines", "rh ", "recrutement", "recruiter", "human resources", "hr ", "talent", "personal"),
        DESIGN("Design et création", "design", "design",
                "design", "graphiste", "ux", "ui ", "creative", "illustrat"),
        INGENIERIE("Ingénierie et industrie", "ingénieur", "engineer",
                "ingenieur", "ingénieur", "industrie", "mecanique", "mécanique", "electrique", "électrique", "mechanical",
                "electrical", "manufacturing", "techniker"),
        SANTE("Santé et social", "santé", "healthcare",
                "sante", "santé", "infirmier", "medical", "médical", "aide-soignant", "nurse", "health", "pflege", "arzt"),
        LOGISTIQUE("Logistique et transport", "logistique", "logistics",
                "logistique", "transport", "supply chain", "entrepot", "entrepôt", "chauffeur", "logistics", "lager"),
        JURIDIQUE("Juridique", "juriste", "legal",
                "juridique", "juriste", "avocat", "legal", "compliance", "lawyer", "recht"),
        SUPPORT("Service client et support", "service client", "customer support",
                "service client", "support", "customer", "conseiller client", "kundenservice"),
        EDUCATION("Éducation et formation", "enseignement", "teacher",
                "enseignant", "professeur", "formateur", "education", "teacher", "tutor", "lehrer"),
        HOTELLERIE("Hôtellerie, restauration et tourisme", "hôtellerie restauration", "hospitality",
                "hotel", "hôtel", "restauration", "cuisinier", "serveur", "tourisme", "hospitality", "chef", "koch"),
        BTP("BTP et construction", "bâtiment", "construction",
                "batiment", "bâtiment", "btp", "chantier", "construction", "architect", "bau");

        private final String libelle;
        private final String motFr;
        private final String motEn;
        private final List<String> reperes;

        Domaine(String libelle, String motFr, String motEn, String... reperes) {
            this.libelle = libelle;
            this.motFr = motFr;
            this.motEn = motEn;
            this.reperes = Arrays.asList(reperes);
        }

        public String libelle() {
            return libelle;
        }

        public String motFr() {
            return motFr;
        }

        public String motEn() {
            return motEn;
        }

        /** Vrai si le texte (en minuscules) contient un mot reperant ce domaine. */
        public boolean reconnu(String texteMinuscule) {
            String t = " " + texteMinuscule + " ";
            return reperes.stream().anyMatch(t::contains);
        }
    }

    public static Optional<Domaine> domaine(String code) {
        return Arrays.stream(Domaine.values()).filter(d -> d.name().equalsIgnoreCase(code)).findFirst();
    }
}
