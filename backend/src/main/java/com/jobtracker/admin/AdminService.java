package com.jobtracker.admin;

import com.jobtracker.candidature.CandidatureRepository;
import com.jobtracker.candidature.StatutCandidature;
import com.jobtracker.common.NotFoundException;
import com.jobtracker.document.DocumentRepository;
import com.jobtracker.user.Administrateurs;
import com.jobtracker.user.AuthService;
import com.jobtracker.user.CompteService;
import com.jobtracker.user.User;
import com.jobtracker.user.UserRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Vue d'ensemble de la plateforme et gestion des comptes, reservees aux administrateurs. */
@Service
public class AdminService {

    static final int SEMAINES = 8;
    /** Motif LIKE des comptes de demonstration, exclus des statistiques. */
    private static final String DEMO = "%" + AuthService.DOMAINE_DEMO;

    private final UserRepository userRepository;
    private final CandidatureRepository candidatureRepository;
    private final DocumentRepository documentRepository;
    private final CompteService compteService;
    private final Administrateurs administrateurs;

    public AdminService(UserRepository userRepository, CandidatureRepository candidatureRepository,
            DocumentRepository documentRepository, CompteService compteService, Administrateurs administrateurs) {
        this.userRepository = userRepository;
        this.candidatureRepository = candidatureRepository;
        this.documentRepository = documentRepository;
        this.compteService = compteService;
        this.administrateurs = administrateurs;
    }

    public record Semaine(LocalDate debut, long inscriptions, long candidatures) {
    }

    /** Chiffres hors comptes de demonstration (sauf comptesDemo). */
    public record Statistiques(long utilisateurs, long comptesDemo, long inscrits7Jours, long inscrits30Jours,
            long candidatures, long documents, Map<StatutCandidature, Long> candidaturesParStatut,
            List<Semaine> activite) {
    }

    /** principal : administrateur defini par ADMIN_EMAILS, dont les droits ne peuvent pas etre retires ici. */
    public record Utilisateur(Long id, String nom, String email, LocalDateTime dateCreation, long candidatures,
            long documents, boolean demo, boolean admin, boolean principal) {
    }

    /** Refuse l'acces (403) si l'utilisateur connecte n'est pas administrateur. */
    @Transactional(readOnly = true)
    public void verifierAdmin(Long userId) {
        if (!administrateurs.estAdmin(userRepository.findById(userId).orElse(null))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces reserve aux administrateurs");
        }
    }

    @Transactional(readOnly = true)
    public Statistiques statistiques() {
        LocalDateTime maintenant = LocalDateTime.now();
        Map<StatutCandidature, Long> parStatut = new LinkedHashMap<>();
        for (StatutCandidature s : StatutCandidature.values()) {
            parStatut.put(s, 0L);
        }
        candidatureRepository.compterParStatut(DEMO).forEach(l -> parStatut.put((StatutCandidature) l[0],
                ((Number) l[1]).longValue()));
        long demo = userRepository.countByEmailEndingWith(AuthService.DOMAINE_DEMO);
        return new Statistiques(
                userRepository.count() - demo,
                demo,
                userRepository.compterInscritsDepuis(maintenant.minusDays(7), DEMO),
                userRepository.compterInscritsDepuis(maintenant.minusDays(30), DEMO),
                parStatut.values().stream().mapToLong(Long::longValue).sum(),
                documentRepository.compterHors(DEMO),
                parStatut,
                activite(maintenant.toLocalDate()));
    }

    /** Inscriptions et candidatures des 8 dernieres semaines (du lundi au dimanche). */
    private List<Semaine> activite(LocalDate aujourdhui) {
        LocalDate premierLundi = aujourdhui.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .minusWeeks(SEMAINES - 1);
        Map<LocalDate, Long> inscriptions = userRepository.datesInscription(premierLundi.atStartOfDay(), DEMO).stream()
                .collect(Collectors.groupingBy(d -> lundi(d.toLocalDate()), Collectors.counting()));
        Map<LocalDate, Long> candidatures = candidatureRepository.datesCandidature(premierLundi, DEMO).stream()
                .collect(Collectors.groupingBy(AdminService::lundi, Collectors.counting()));
        List<Semaine> semaines = new ArrayList<>();
        for (int i = 0; i < SEMAINES; i++) {
            LocalDate debut = premierLundi.plusWeeks(i);
            semaines.add(new Semaine(debut, inscriptions.getOrDefault(debut, 0L),
                    candidatures.getOrDefault(debut, 0L)));
        }
        return semaines;
    }

    private static LocalDate lundi(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    @Transactional(readOnly = true)
    public List<Utilisateur> utilisateurs() {
        Map<Long, Long> candidatures = compter(candidatureRepository.compterParUtilisateur());
        Map<Long, Long> documents = compter(documentRepository.compterParUtilisateur());
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getDateCreation, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(u -> new Utilisateur(u.getId(), u.getNom(), u.getEmail(), u.getDateCreation(),
                        candidatures.getOrDefault(u.getId(), 0L), documents.getOrDefault(u.getId(), 0L),
                        u.getEmail().endsWith(AuthService.DOMAINE_DEMO), administrateurs.estAdmin(u),
                        administrateurs.estPrincipal(u.getEmail())))
                .toList();
    }

    private static Map<Long, Long> compter(List<Object[]> lignes) {
        return lignes.stream().collect(Collectors.toMap(l -> ((Number) l[0]).longValue(),
                l -> ((Number) l[1]).longValue()));
    }

    /** Deconnecte toutes les sessions d'un compte (en cas de doute sur sa securite). */
    @Transactional
    public void deconnecter(Long userId) {
        userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Compte introuvable"))
                .revoquerJetons();
    }

    /** Supprime un compte et ses donnees ; un administrateur ne peut pas etre supprime d'ici. */
    @Transactional
    public void supprimer(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Compte introuvable"));
        if (administrateurs.estAdmin(user)) {
            throw new IllegalArgumentException("Retire d'abord ses droits d'administrateur pour supprimer ce compte");
        }
        compteService.effacer(userId);
    }

    /**
     * Nomme un utilisateur administrateur, ou lui retire ce role. Garde-fous : on ne modifie pas son propre role
     * (pour ne pas perdre l'acces), ni celui d'un administrateur principal (ADMIN_EMAILS), et un compte de
     * demonstration ne peut pas devenir administrateur.
     */
    @Transactional
    public void changerRole(Long adminId, Long userId, boolean admin) {
        if (adminId.equals(userId)) {
            throw new IllegalArgumentException("Tu ne peux pas modifier ton propre role");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Compte introuvable"));
        if (administrateurs.estPrincipal(user.getEmail())) {
            throw new IllegalArgumentException("Ce compte est administrateur principal (ADMIN_EMAILS) : son role ne se modifie que sur Render");
        }
        if (admin && user.getEmail().endsWith(AuthService.DOMAINE_DEMO)) {
            throw new IllegalArgumentException("Un compte de demonstration ne peut pas etre administrateur");
        }
        user.definirAdministrateur(admin);
    }
}
