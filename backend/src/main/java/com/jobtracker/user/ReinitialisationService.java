package com.jobtracker.user;

import com.jobtracker.common.EmailService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Mot de passe oublie" : un lien a usage unique, valable 30 minutes, est envoye par email.
 * La reponse est toujours la meme, que le compte existe ou non (on ne revele pas les emails inscrits).
 */
@Service
public class ReinitialisationService {

    static final int VALIDITE_MINUTES = 30;
    static final int MAX_DEMANDES_PAR_HEURE = 3;

    private final SecureRandom aleatoire = new SecureRandom();
    private final UserRepository userRepository;
    private final JetonReinitialisationRepository jetonRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TentativesConnexion tentatives;
    private final String frontendUrl;

    public ReinitialisationService(UserRepository userRepository, JetonReinitialisationRepository jetonRepository,
            PasswordEncoder passwordEncoder, EmailService emailService, TentativesConnexion tentatives,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.userRepository = userRepository;
        this.jetonRepository = jetonRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.tentatives = tentatives;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    @Transactional
    public void demander(String email) {
        String adresse = email.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(adresse).orElse(null);
        if (user == null || adresse.endsWith(AuthService.DOMAINE_DEMO)) {
            return;
        }
        // Limite anti-abus : 3 emails par heure et par compte
        if (jetonRepository.countByUserIdAndDateCreationAfter(user.getId(), LocalDateTime.now().minusHours(1))
                >= MAX_DEMANDES_PAR_HEURE) {
            return;
        }
        byte[] octets = new byte[32];
        aleatoire.nextBytes(octets);
        String jeton = Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
        jetonRepository.save(new JetonReinitialisation(user, hash(jeton),
                LocalDateTime.now().plusMinutes(VALIDITE_MINUTES)));

        String lien = frontendUrl + "/reinitialiser?jeton=" + jeton;
        String texte = """
                Bonjour %s,

                Tu as demande a reinitialiser ton mot de passe Postilyn. Clique sur ce lien (valable %d minutes, \
                utilisable une seule fois) :
                %s

                Si tu n'es pas a l'origine de cette demande, ignore cet email : ton mot de passe ne change pas.
                """.formatted(user.getNom(), VALIDITE_MINUTES, lien);
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:520px;margin:auto;color:#1e293b">
                  <h2 style="color:#6366f1">Postilyn</h2>
                  <p>Bonjour %s,</p>
                  <p>Tu as demandé à réinitialiser ton mot de passe. Ce lien est valable <b>%d minutes</b> \
                et ne peut servir qu'une fois.</p>
                  <p style="text-align:center;margin:28px 0">
                    <a href="%s" style="background:linear-gradient(90deg,#6366f1,#d946ef);color:#fff;padding:12px 22px;\
                border-radius:999px;text-decoration:none;font-weight:bold">Choisir un nouveau mot de passe</a>
                  </p>
                  <p style="font-size:13px;color:#64748b">Si tu n'es pas à l'origine de cette demande, ignore cet \
                email : ton mot de passe ne change pas.</p>
                </div>
                """.formatted(echapper(user.getNom()), VALIDITE_MINUTES, lien);
        emailService.envoyer(user.getEmail(), "Réinitialise ton mot de passe Postilyn", texte, html);
    }

    @Transactional
    public void reinitialiser(String jeton, String nouveau) {
        JetonReinitialisation j = jetonRepository.findByHash(hash(jeton == null ? "" : jeton.strip()))
                .filter(JetonReinitialisation::valide)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ce lien est invalide ou a expire. Fais une nouvelle demande de reinitialisation."));
        User user = j.getUser();
        user.changerMotDePasse(passwordEncoder.encode(nouveau));
        user.revoquerJetons();
        // Le lien utilise et les autres liens encore valides de ce compte ne servent plus
        jetonRepository.findByUserIdAndUtiliseFalse(user.getId()).forEach(JetonReinitialisation::utiliser);
        tentatives.succes(user.getEmail());
    }

    static String hash(String jeton) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(jeton.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String echapper(String texte) {
        return texte.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
