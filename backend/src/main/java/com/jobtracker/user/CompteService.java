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

    @Transactional
    public void changerMotDePasse(Long userId, String actuel, String nouveau) {
        User user = verifier(userId, actuel);
        user.changerMotDePasse(passwordEncoder.encode(nouveau));
    }

    @Transactional(readOnly = true)
    public Export exporter(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Compte introuvable"));
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
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Compte introuvable"));
        if (motDePasse == null || !passwordEncoder.matches(motDePasse, user.getPassword())) {
            throw new BadCredentialsException("Mot de passe incorrect");
        }
        return user;
    }
}
