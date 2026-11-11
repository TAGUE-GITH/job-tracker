package com.jobtracker.support;

import com.jobtracker.common.EmailService;
import com.jobtracker.common.NotFoundException;
import com.jobtracker.support.MessageContact.Categorie;
import com.jobtracker.user.Administrateurs;
import com.jobtracker.user.User;
import com.jobtracker.user.UserRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

/** Page Contact (messages au support) et avis des utilisateurs, consultes dans l'administration. */
@Service
public class SupportService {

    static final int MESSAGES_PAR_HEURE = 5;
    static final int AVIS_PAR_JOUR = 3;

    private final MessageContactRepository messageRepository;
    private final AvisRepository avisRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final Administrateurs administrateurs;
    private final String emailContact;

    public SupportService(MessageContactRepository messageRepository, AvisRepository avisRepository,
            UserRepository userRepository, EmailService emailService, Administrateurs administrateurs,
            @Value("${app.contact.email:}") String emailContact) {
        this.messageRepository = messageRepository;
        this.avisRepository = avisRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.administrateurs = administrateurs;
        this.emailContact = emailContact.strip();
    }

    public record MessageVue(Long id, Long userId, String nom, String email, Categorie categorie, String sujet,
            String message, LocalDateTime dateCreation, boolean traite) {

        static MessageVue de(MessageContact m) {
            return new MessageVue(m.getId(), m.getUserId(), m.getNom(), m.getEmail(), m.getCategorie(), m.getSujet(),
                    m.getMessage(), m.getDateCreation(), m.isTraite());
        }
    }

    public record AvisVue(Long id, String nom, String email, int note, String commentaire, LocalDateTime dateCreation) {
    }

    public record SyntheseAvis(long nombre, double moyenne, Map<Integer, Long> parNote, List<AvisVue> avis) {
    }

    /** Enregistre le message, previent l'equipe par email et envoie un accuse de reception. */
    @Transactional
    public MessageVue envoyerMessage(Long userId, String nom, String email, Categorie categorie, String sujet,
            String message) {
        String adresse = email.strip().toLowerCase();
        if (messageRepository.countByEmailIgnoreCaseAndDateCreationAfter(adresse, LocalDateTime.now().minusHours(1))
                >= MESSAGES_PAR_HEURE) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Tu as deja envoye plusieurs messages : reessaie dans une heure.");
        }
        MessageContact m = messageRepository.save(
                new MessageContact(userId, nom.strip(), adresse, categorie, sujet.strip(), message.strip()));
        prevenirEquipe(m);
        emailService.envoyer(adresse, "Postulyn : nous avons bien reçu ton message",
                "Bonjour " + m.getNom() + ",\n\nNous avons bien reçu ton message « " + m.getSujet()
                        + " ». Nous te répondons dès que possible à cette adresse.\n\nL'équipe Postulyn",
                "<p>Bonjour " + html(m.getNom()) + ",</p><p>Nous avons bien reçu ton message « <b>" + html(m.getSujet())
                        + "</b> ». Nous te répondons dès que possible à cette adresse.</p><p>L'équipe Postulyn</p>");
        return MessageVue.de(m);
    }

    private void prevenirEquipe(MessageContact m) {
        Set<String> destinataires = new LinkedHashSet<>();
        if (!emailContact.isBlank()) {
            destinataires.add(emailContact);
        } else {
            destinataires.addAll(administrateurs.principaux());
        }
        String sujet = "[Postulyn · " + m.getCategorie() + "] " + m.getSujet();
        String texte = "De : " + m.getNom() + " <" + m.getEmail() + ">" + (m.getUserId() != null ? " (compte n°"
                + m.getUserId() + ")" : " (visiteur)") + "\n\n" + m.getMessage()
                + "\n\nRéponds directement à " + m.getEmail() + ", puis marque le message comme traité dans "
                + "Administration > Messages.";
        String corps = "<p><b>De :</b> " + html(m.getNom()) + " &lt;" + html(m.getEmail()) + "&gt;</p><p>"
                + html(m.getMessage()).replace("\n", "<br>") + "</p>";
        destinataires.forEach(d -> emailService.envoyer(d, sujet, texte, corps));
    }

    private static String html(String texte) {
        return HtmlUtils.htmlEscape(texte == null ? "" : texte);
    }

    @Transactional
    public void donnerAvis(Long userId, int note, String commentaire) {
        if (note < 1 || note > 5) {
            throw new IllegalArgumentException("La note doit etre comprise entre 1 et 5");
        }
        if (avisRepository.countByUserIdAndDateCreationAfter(userId, LocalDateTime.now().minusDays(1)) >= AVIS_PAR_JOUR) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Merci, tu as deja donne ton avis aujourd'hui.");
        }
        String c = commentaire == null || commentaire.isBlank() ? null : commentaire.strip();
        avisRepository.save(new Avis(userId, note, c));
    }

    @Transactional(readOnly = true)
    public List<MessageVue> messages() {
        return messageRepository.findAllByOrderByTraiteAscDateCreationDesc().stream().map(MessageVue::de).toList();
    }

    @Transactional(readOnly = true)
    public long messagesNonTraites() {
        return messageRepository.countByTraiteFalse();
    }

    @Transactional
    public void marquerTraite(Long id, boolean traite) {
        messageRepository.findById(id).orElseThrow(() -> new NotFoundException("Message introuvable"))
                .marquerTraite(traite);
    }

    @Transactional
    public void supprimerMessage(Long id) {
        messageRepository.delete(messageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Message introuvable")));
    }

    @Transactional(readOnly = true)
    public SyntheseAvis avis() {
        List<Avis> tous = avisRepository.findAllByOrderByDateCreationDesc();
        Map<Long, User> auteurs = userRepository.findAllById(tous.stream().map(Avis::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Integer, Long> parNote = new java.util.TreeMap<>();
        for (int n = 1; n <= 5; n++) {
            parNote.put(n, 0L);
        }
        tous.forEach(a -> parNote.merge(a.getNote(), 1L, Long::sum));
        double moyenne = tous.stream().mapToInt(Avis::getNote).average().orElse(0);
        List<AvisVue> vues = tous.stream().map(a -> {
            User u = auteurs.get(a.getUserId());
            return new AvisVue(a.getId(), u != null ? u.getNom() : "Compte supprimé", u != null ? u.getEmail() : null,
                    a.getNote(), a.getCommentaire(), a.getDateCreation());
        }).toList();
        return new SyntheseAvis(tous.size(), Math.round(moyenne * 10) / 10.0, parNote, vues);
    }
}
