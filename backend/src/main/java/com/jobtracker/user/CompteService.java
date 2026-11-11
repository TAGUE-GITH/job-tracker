package com.jobtracker.user;

import com.jobtracker.candidature.CandidatureDtos.CandidatureResponse;
import com.jobtracker.candidature.CandidatureService;
import com.jobtracker.common.NotFoundException;
import com.jobtracker.document.DocumentResponse;
import com.jobtracker.common.EmailService;
import com.jobtracker.document.DocumentService;
import com.jobtracker.user.AuthDtos.AuthResponse;
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
    private final AuthService authService;
    private final JetonReinitialisationRepository jetonRepository;
    private final EmailService emailService;
    private final Administrateurs administrateurs;
    private final PhotoProfilRepository photoRepository;

    public CompteService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            CandidatureService candidatureService, DocumentService documentService, AuthService authService,
            JetonReinitialisationRepository jetonRepository, EmailService emailService,
            Administrateurs administrateurs, PhotoProfilRepository photoRepository) {
        this.administrateurs = administrateurs;
        this.photoRepository = photoRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.candidatureService = candidatureService;
        this.documentService = documentService;
        this.authService = authService;
        this.jetonRepository = jetonRepository;
        this.emailService = emailService;
    }

    public record Export(Instant exporteLe, String nom, String email, List<CandidatureResponse> candidatures,
            List<DocumentResponse> documents) {
    }

    /** admin : vrai si l'email figure dans ADMIN_EMAILS (affiche le menu Administration). photo : photo enregistree. */
    public record Profil(String nom, String email, String telephone, String adresse, String ville, boolean admin,
            boolean photo) {
    }

    private Profil profilDe(User u) {
        return new Profil(u.getNom(), u.getEmail(), u.getTelephone(), u.getAdresse(), u.getVille(),
                administrateurs.estAdmin(u), photoRepository.existsById(u.getId()));
    }

    static final long TAILLE_MAX_PHOTO = 3 * 1024 * 1024;

    /** Enregistre la photo du CV apres avoir verifie que c'est bien une image JPEG ou PNG. */
    @Transactional
    public void enregistrerPhoto(Long userId, byte[] donnees, String contentType) {
        utilisateur(userId);
        if (donnees == null || donnees.length == 0) {
            throw new IllegalArgumentException("Choisis une photo");
        }
        if (donnees.length > TAILLE_MAX_PHOTO) {
            throw new IllegalArgumentException("Photo trop lourde : 3 Mo maximum");
        }
        String type = contentType == null ? "" : contentType.toLowerCase(java.util.Locale.ROOT);
        if (!type.equals("image/jpeg") && !type.equals("image/png")) {
            throw new IllegalArgumentException("Format accepte : JPEG ou PNG");
        }
        try {
            if (javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(donnees)) == null) {
                throw new IllegalArgumentException("Ce fichier n'est pas une image lisible");
            }
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Ce fichier n'est pas une image lisible");
        }
        photoRepository.save(new PhotoProfil(userId, donnees, type));
    }

    @Transactional(readOnly = true)
    public java.util.Optional<PhotoProfil> photo(Long userId) {
        return photoRepository.findById(userId);
    }

    @Transactional
    public void supprimerPhoto(Long userId) {
        photoRepository.deleteById(userId);
    }

    @Transactional(readOnly = true)
    public Profil profil(Long userId) {
        return profilDe(utilisateur(userId));
    }

    @Transactional
    public Profil modifierProfil(Long userId, String nom, String telephone, String adresse, String ville) {
        User user = utilisateur(userId);
        user.modifierProfil(nom.strip(), vide(telephone), vide(adresse), vide(ville));
        return profilDe(user);
    }

    private static String vide(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.strip();
    }

    private User utilisateur(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Compte introuvable"));
    }

    /** Change le mot de passe et deconnecte les autres appareils ; renvoie un jeton neuf pour celui-ci. */
    @Transactional
    public AuthResponse changerMotDePasse(Long userId, String actuel, String nouveau) {
        User user = verifier(userId, actuel);
        user.changerMotDePasse(passwordEncoder.encode(nouveau));
        user.revoquerJetons();
        return authService.toResponse(user);
    }

    /** Envoie un email de test a l'adresse du compte et renvoie le diagnostic de Brevo. */
    @Transactional(readOnly = true)
    public EmailService.Envoi emailTest(Long userId) {
        User user = utilisateur(userId);
        if (user.getEmail().endsWith(AuthService.DOMAINE_DEMO)) {
            return new EmailService.Envoi(false, "Les comptes de demonstration n'ont pas de vraie adresse email.");
        }
        return emailService.envoyerAvecDetail(user.getEmail(), "Test d'envoi Postulyn",
                "Bonjour " + user.getNom() + ",\n\nSi tu lis ce message, l'envoi d'emails de Postulyn fonctionne.",
                "<p>Bonjour " + user.getNom().replace("<", "&lt;") + ",</p><p>Si tu lis ce message, l'envoi "
                        + "d'emails de <b>Postulyn</b> fonctionne. ✅</p>");
    }

    /** Deconnecte toutes les sessions ouvertes ; renvoie un jeton neuf pour l'appareil courant. */
    @Transactional
    public AuthResponse deconnecterPartout(Long userId) {
        User user = utilisateur(userId);
        user.revoquerJetons();
        return authService.toResponse(user);
    }

    @Transactional(readOnly = true)
    public Export exporter(Long userId) {
        User user = utilisateur(userId);
        return new Export(Instant.now(), user.getNom(), user.getEmail(),
                candidatureService.lister(userId, null, null), documentService.lister(userId));
    }

    @Transactional
    public void supprimer(Long userId, String motDePasse) {
        verifier(userId, motDePasse);
        effacer(userId);
    }

    /** Supprime definitivement le compte et toutes ses donnees (sans demander le mot de passe). */
    @Transactional
    public void effacer(Long userId) {
        User user = utilisateur(userId);
        candidatureService.toutes(userId).forEach(c -> candidatureService.supprimer(userId, c.getId()));
        documentService.lister(userId).forEach(d -> documentService.supprimer(userId, d.id()));
        jetonRepository.supprimerPourUtilisateur(userId);
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
