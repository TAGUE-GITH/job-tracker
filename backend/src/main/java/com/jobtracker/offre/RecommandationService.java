package com.jobtracker.offre;

import com.jobtracker.common.NotFoundException;
import com.jobtracker.document.DocumentService;
import com.jobtracker.ia.IaService;
import com.jobtracker.offre.OffreDtos.OffreExterne;
import com.jobtracker.offre.OffreDtos.OffresResponse;
import com.jobtracker.offre.OffreDtos.Recherche;
import com.jobtracker.user.User;
import com.jobtracker.user.UserRepository;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * "Offres pour moi" : l'IA lit le CV une fois et en tire un profil (metiers, competences...). Les offres sont
 * ensuite cherchees avec les metiers du profil puis notees selon les competences qu'elles mentionnent.
 */
@Service
public class RecommandationService {

    private static final Logger LOG = LoggerFactory.getLogger(RecommandationService.class);
    private static final JsonMapper JSON = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    private static final int MAX_RECOMMANDATIONS = 40;

    private final IaService iaService;
    private final OffreService offreService;
    private final UserRepository userRepository;
    private final DocumentService documentService;

    public RecommandationService(IaService iaService, OffreService offreService, UserRepository userRepository,
            DocumentService documentService) {
        this.iaService = iaService;
        this.offreService = offreService;
        this.userRepository = userRepository;
        this.documentService = documentService;
    }

    /** Profil du candidat tel que l'IA l'a compris. */
    public record ProfilCv(String titre, String resume, List<String> metiers, List<String> competences,
            List<String> motsCles, String domaine, String niveau, List<String> contrats, Long cvId, String nomCv) {

        List<String> metiersOuTitre() {
            List<String> l = new ArrayList<>(liste(metiers));
            if (l.isEmpty() && titre != null && !titre.isBlank()) {
                l.add(titre);
            }
            return l.stream().limit(3).toList();
        }
    }

    public record OffreRecommandee(OffreExterne offre, int score, List<String> motsTrouves) {
    }

    public record Recommandations(ProfilCv profil, List<OffreRecommandee> offres, String remarque) {
    }

    @Transactional
    public ProfilCv analyser(Long userId, Long cvId) {
        String nomCv = documentService.lister(userId).stream().filter(d -> d.id().equals(cvId)).findFirst()
                .orElseThrow(() -> new NotFoundException("CV introuvable")).nomFichier();
        String json = iaService.profilCv(userId, cvId).reponse();
        ProfilCv profil = lire(json, cvId, nomCv);
        String stocke = JSON.writeValueAsString(profil);
        utilisateur(userId).definirProfilCv(stocke.length() > 8000 ? stocke.substring(0, 8000) : stocke, cvId);
        return profil;
    }

    static ProfilCv lire(String json, Long cvId, String nomCv) {
        ProfilCv p = JSON.readValue(json, ProfilCv.class);
        return new ProfilCv(p.titre(), p.resume(), liste(p.metiers()), liste(p.competences()), liste(p.motsCles()),
                p.domaine(), p.niveau(), liste(p.contrats()), cvId != null ? cvId : p.cvId(),
                nomCv != null ? nomCv : p.nomCv());
    }

    @Transactional(readOnly = true)
    public ProfilCv profil(Long userId) {
        String json = utilisateur(userId).getProfilCv();
        return json == null ? null : lire(json, null, null);
    }

    @Transactional
    public void oublier(Long userId) {
        utilisateur(userId).definirProfilCv(null, null);
    }

    private User utilisateur(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Compte introuvable"));
    }

    /** Cherche avec chaque metier du profil, fusionne, puis classe par correspondance (ou par date). */
    public Recommandations recommander(Long userId, String pays, String contrat, String lieu, boolean parDate) {
        ProfilCv profil = profil(userId);
        if (profil == null) {
            throw new IllegalArgumentException("Analyse d'abord ton CV pour recevoir des offres qui te correspondent");
        }
        Catalogue.Domaine domaine = profil.domaine() == null ? null : Catalogue.domaine(profil.domaine()).orElse(null);
        List<OffreExterne> trouvees = new ArrayList<>();
        Set<String> liens = new HashSet<>();
        List<String> erreurs = new ArrayList<>();
        for (String metier : profil.metiersOuTitre()) {
            try {
                OffresResponse res = offreService.rechercher(OffreService.TOUTES,
                        new Recherche(metier, lieu, contrat, 0, pays, domaine, parDate));
                res.offres().stream().filter(o -> o.url() == null || liens.add(o.url())).forEach(trouvees::add);
            } catch (RuntimeException e) {
                LOG.info("Recherche '{}' impossible : {}", metier, e.getMessage());
                erreurs.add(e.getMessage());
            }
        }
        if (trouvees.isEmpty() && !erreurs.isEmpty()) {
            throw new OffresIndisponiblesException(erreurs.getFirst());
        }
        List<OffreRecommandee> notees = trouvees.stream().map(o -> noter(o, profil)).toList();
        Comparator<OffreRecommandee> ordre = parDate
                ? Comparator.comparing((OffreRecommandee r) -> r.offre().datePublication(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                : Comparator.comparingInt(OffreRecommandee::score).reversed();
        List<OffreRecommandee> triees = notees.stream()
                .filter(r -> r.score() >= 20)
                .sorted(ordre)
                .limit(MAX_RECOMMANDATIONS)
                .toList();
        String remarque = "Recherches : " + String.join(", ", profil.metiersOuTitre())
                + ". Offres classées selon les compétences de ton CV qu'elles mentionnent.";
        return new Recommandations(profil, triees, remarque);
    }

    /**
     * Note de 0 a 100 : jusqu'a 45 points si le titre de l'offre reprend un metier du profil, jusqu'a 55 points
     * selon le nombre de competences et mots-cles du profil presents dans l'offre.
     */
    static OffreRecommandee noter(OffreExterne o, ProfilCv profil) {
        String titre = normaliser(o.titre());
        String texte = " " + titre + " " + normaliser(o.description()) + " ";
        double meilleurTitre = 0;
        for (String metier : profil.metiersOuTitre()) {
            List<String> mots = mots(metier);
            if (!mots.isEmpty()) {
                long presents = mots.stream().filter(m -> (" " + titre + " ").contains(" " + m)).count();
                meilleurTitre = Math.max(meilleurTitre, (double) presents / mots.size());
            }
        }
        Set<String> termes = new LinkedHashSet<>();
        termes.addAll(liste(profil.competences()));
        termes.addAll(liste(profil.motsCles()));
        List<String> trouves = termes.stream()
                .filter(t -> {
                    String n = normaliser(t);
                    return n.length() >= 2 && texte.contains(n.length() <= 3 ? " " + n + " " : n);
                })
                .toList();
        double partCompetences = Math.min(1.0, trouves.size() / 5.0);
        int score = (int) Math.round(45 * meilleurTitre + 55 * partCompetences);
        return new OffreRecommandee(o, Math.min(100, score), trouves.stream().limit(8).toList());
    }

    /** Minuscules sans accents ni ponctuation superflue. */
    static String normaliser(String texte) {
        if (texte == null) {
            return "";
        }
        String sansAccents = Normalizer.normalize(texte, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sansAccents.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9+#.]+", " ").strip();
    }

    /** Mots significatifs d'un intitule (sans "de", "et", "h/f"...). */
    private static List<String> mots(String intitule) {
        Set<String> vides = Set.of("de", "du", "des", "la", "le", "les", "et", "en", "a", "h", "f", "x", "the", "of",
                "and", "junior", "senior");
        return List.of(normaliser(intitule).split(" ")).stream().filter(m -> m.length() > 1 && !vides.contains(m))
                .toList();
    }

    private static <T> List<T> liste(List<T> l) {
        return l == null ? List.of() : l;
    }
}
