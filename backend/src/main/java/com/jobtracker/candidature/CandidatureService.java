package com.jobtracker.candidature;

import com.jobtracker.candidature.CandidatureDtos.CandidatureRequest;
import com.jobtracker.candidature.CandidatureDtos.CandidatureResponse;
import com.jobtracker.common.NotFoundException;
import com.jobtracker.document.Document;
import com.jobtracker.document.DocumentService;
import com.jobtracker.document.TypeDocument;
import com.jobtracker.etape.Etape;
import com.jobtracker.etape.EtapeDtos.EtapeRequest;
import com.jobtracker.etape.TypeEtape;
import com.jobtracker.user.UserRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CandidatureService {

    private final CandidatureRepository candidatureRepository;
    private final UserRepository userRepository;
    private final DocumentService documentService;

    public CandidatureService(CandidatureRepository candidatureRepository, UserRepository userRepository,
            DocumentService documentService) {
        this.candidatureRepository = candidatureRepository;
        this.userRepository = userRepository;
        this.documentService = documentService;
    }

    public List<CandidatureResponse> lister(Long userId, StatutCandidature statut, String recherche) {
        String q = recherche == null ? "" : recherche.trim().toLowerCase(Locale.ROOT);
        return candidatureRepository.findByUserIdOrderByDateCandidatureDescIdDesc(userId).stream()
                .filter(c -> statut == null || c.getStatut() == statut)
                .filter(c -> q.isEmpty()
                        || c.getNomEntreprise().toLowerCase(Locale.ROOT).contains(q)
                        || c.getPoste().toLowerCase(Locale.ROOT).contains(q))
                .map(CandidatureResponse::from)
                .toList();
    }

    public List<Candidature> toutes(Long userId) {
        return candidatureRepository.findByUserIdOrderByDateCandidatureDescIdDesc(userId);
    }

    public CandidatureResponse detail(Long userId, Long id) {
        return CandidatureResponse.from(trouver(userId, id));
    }

    public Candidature trouver(Long userId, Long id) {
        return candidatureRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Candidature introuvable"));
    }

    @Transactional
    public CandidatureResponse creer(Long userId, CandidatureRequest req) {
        Candidature c = new Candidature(userRepository.getReferenceById(userId));
        appliquer(userId, c, req);
        c.ajouterEtape(new Etape(TypeEtape.CANDIDATURE_ENVOYEE, req.dateCandidature(), null));
        appliquerAbandon(c, req.abandonnee());
        return CandidatureResponse.from(candidatureRepository.save(c));
    }

    @Transactional
    public CandidatureResponse modifier(Long userId, Long id, CandidatureRequest req) {
        Candidature c = trouver(userId, id);
        appliquer(userId, c, req);
        appliquerAbandon(c, req.abandonnee());
        return CandidatureResponse.from(c);
    }

    /** Enregistre la lettre de motivation (redigee avec TAGIA) en PDF et la rattache a la candidature. */
    @Transactional
    public CandidatureResponse enregistrerLettre(Long userId, Long id, String texte) {
        Candidature c = trouver(userId, id);
        String nom = ("Lettre de motivation - " + c.getNomEntreprise()).replaceAll("[\\\\/:*?\"<>|]", " ").strip();
        c.setLettre(documentService.creerLettrePdf(userId, nom + ".pdf", texte));
        return CandidatureResponse.from(c);
    }

    @Transactional
    public void supprimer(Long userId, Long id) {
        candidatureRepository.delete(trouver(userId, id));
    }

    @Transactional
    public CandidatureResponse ajouterEtape(Long userId, Long candidatureId, EtapeRequest req) {
        Candidature c = trouver(userId, candidatureId);
        c.ajouterEtape(new Etape(req.type(), req.date(), blankToNull(req.notes())));
        candidatureRepository.flush();
        return CandidatureResponse.from(c);
    }

    @Transactional
    public CandidatureResponse modifierEtape(Long userId, Long candidatureId, Long etapeId, EtapeRequest req) {
        Candidature c = trouver(userId, candidatureId);
        trouverEtape(c, etapeId).modifier(req.type(), req.date(), blankToNull(req.notes()));
        c.recalculerStatut();
        return CandidatureResponse.from(c);
    }

    @Transactional
    public CandidatureResponse supprimerEtape(Long userId, Long candidatureId, Long etapeId) {
        Candidature c = trouver(userId, candidatureId);
        c.retirerEtape(trouverEtape(c, etapeId));
        return CandidatureResponse.from(c);
    }

    private Etape trouverEtape(Candidature c, Long etapeId) {
        return c.getEtapes().stream()
                .filter(e -> e.getId().equals(etapeId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Etape introuvable"));
    }

    private void appliquer(Long userId, Candidature c, CandidatureRequest req) {
        c.setNomEntreprise(req.nomEntreprise().trim());
        c.setPoste(req.poste().trim());
        c.setLienOffre(blankToNull(req.lienOffre()));
        c.setTexteOffre(blankToNull(req.texteOffre()));
        c.setDateCandidature(req.dateCandidature());
        c.setCv(document(userId, req.cvId(), TypeDocument.CV));
        c.setLettre(document(userId, req.lettreId(), TypeDocument.LETTRE));
    }

    private void appliquerAbandon(Candidature c, Boolean abandonnee) {
        if (Boolean.TRUE.equals(abandonnee)) {
            c.setStatut(StatutCandidature.ABANDONNEE);
        } else if (c.getStatut() == StatutCandidature.ABANDONNEE) {
            c.setStatut(StatutCandidature.EN_ATTENTE);
            c.recalculerStatut();
        }
    }

    private Document document(Long userId, Long documentId, TypeDocument attendu) {
        if (documentId == null) {
            return null;
        }
        Document doc = documentService.trouver(userId, documentId);
        if (doc.getType() != attendu) {
            throw new IllegalArgumentException("Le document " + documentId + " n'est pas de type " + attendu);
        }
        return doc;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
