package com.jobtracker.user;

import com.jobtracker.candidature.CandidatureDtos.CandidatureResponse;
import com.jobtracker.candidature.CandidatureService;
import com.jobtracker.common.NotFoundException;
import com.jobtracker.document.DocumentResponse;
import com.jobtracker.document.DocumentService;
import java.time.Instant;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestion du compte : mot de passe, export des donnees (RGPD) et suppression definitive. */
@Service
public class CompteService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CandidatureService candidatureService;
    private final DocumentService documentService;

    public CompteService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            CandidatureService candidatureService, DocumentService documentService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.candidatureService = candidatureService;
        this.documentService = documentService;
    }

    public record Export(Instant exporteLe, String nom, String email, List<CandidatureResponse> candidatures,
            List<DocumentResponse> documents) {
    }

    public record Profil(String nom, String email, String telephone, String adresse, String ville) {

        static Profil de(User u) {
            return new Profil(u.getNom(), u.getEmail(), u.getTelephone(), u.getAdresse(), u.getVille());
        }
    }

    @Transactional(readOnly = true)
    public Profil profil(Long userId) {
        return Profil.de(utilisateur(userId));
    }

    @Transactional
    public Profil modifierProfil(Long userId, String nom, String telephone, String adresse, String ville) {
        User user = utilisateur(userId);
        user.modifierProfil(nom.strip(), vide(telephone), vide(adresse), vide(ville));
        return Profil.de(user);
    }

    private static String vide(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.strip();
    }

    private User utilisateur(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Compte introuvable"));
    }

    @Transactional
    public void changerMotDePasse(Long userId, String actuel, String nouveau) {
        User user = verifier(userId, actuel);
        user.changerMotDePasse(passwordEncoder.encode(nouveau));
    }

    @Transactional(readOnly = true)
    public Export exporter(Long userId) {
        User user = utilisateur(userId);
        return new Export(Instant.now(), user.getNom(), user.getEmail(),
                candidatureService.lister(userId, null, null), documentService.lister(userId));
    }

    @Transactional
    public void supprimer(Long userId, String motDePasse) {
        User user = verifier(userId, motDePasse);
        candidatureService.toutes(userId).forEach(c -> candidatureService.supprimer(userId, c.getId()));
        documentService.lister(userId).forEach(d -> documentService.supprimer(userId, d.id()));
        userRepository.delete(user);
    }

    private User verifier(Long userId, String motDePasse) {
        User user = utilisateur(userId);
        if (motDePasse == null || !passwordEncoder.matches(motDePasse, user.getPassword())) {
            throw new BadCredentialsException("Mot de passe incorrect");
        }
        return user;
    }
}
