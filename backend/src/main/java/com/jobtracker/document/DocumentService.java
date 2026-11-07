package com.jobtracker.document;

import com.jobtracker.candidature.CandidatureRepository;
import com.jobtracker.common.NotFoundException;
import com.jobtracker.user.UserRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentContenuRepository contenuRepository;
    private final CandidatureRepository candidatureRepository;
    private final UserRepository userRepository;

    public DocumentService(DocumentRepository documentRepository, DocumentContenuRepository contenuRepository,
            CandidatureRepository candidatureRepository, UserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.contenuRepository = contenuRepository;
        this.candidatureRepository = candidatureRepository;
        this.userRepository = userRepository;
    }

    public List<DocumentResponse> lister(Long userId) {
        return documentRepository.findByUserIdOrderByDateAjoutDesc(userId).stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @Transactional
    public DocumentResponse upload(Long userId, MultipartFile fichier, TypeDocument type) {
        if (fichier == null || fichier.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide");
        }
        if (fichier.getSize() > DocumentContenu.TAILLE_MAX) {
            throw new IllegalArgumentException("Le fichier depasse 10 Mo");
        }
        String nomOriginal = StringUtils.cleanPath(
                fichier.getOriginalFilename() == null ? "document" : fichier.getOriginalFilename());
        byte[] donnees;
        try {
            donnees = fichier.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de lire le fichier", e);
        }
        Document doc = documentRepository.save(new Document(nomOriginal, type, fichier.getContentType(),
                userRepository.getReferenceById(userId)));
        contenuRepository.save(new DocumentContenu(doc, donnees));
        return DocumentResponse.from(doc);
    }

    /** Texte d'un CV, pour que l'IA puisse le comparer a une offre. */
    @Transactional(readOnly = true)
    public String texte(Long userId, Long documentId) {
        Document doc = trouver(userId, documentId);
        byte[] donnees = contenuRepository.findById(doc.getId())
                .map(DocumentContenu::getDonnees)
                .orElseThrow(() -> new NotFoundException("Contenu du document introuvable"));
        return TexteDocument.extraire(donnees, doc.getNomFichier(), doc.getContentType());
    }

    /** Enregistre une lettre de motivation (texte) sous forme de PDF dans les documents de l'utilisateur. */
    @Transactional
    public Document creerLettrePdf(Long userId, String nomFichier, String texte) {
        if (texte == null || texte.isBlank()) {
            throw new IllegalArgumentException("La lettre est vide");
        }
        Document doc = documentRepository.save(new Document(nomFichier, TypeDocument.LETTRE, "application/pdf",
                userRepository.getReferenceById(userId)));
        contenuRepository.save(new DocumentContenu(doc, LettrePdf.generer(texte)));
        return doc;
    }

    public Document trouver(Long userId, Long documentId) {
        return documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new NotFoundException("Document introuvable"));
    }

    @Transactional(readOnly = true)
    public Resource fichier(Document doc) {
        return contenuRepository.findById(doc.getId())
                .map(c -> new ByteArrayResource(c.getDonnees()))
                .orElseThrow(() -> new NotFoundException("Contenu du document introuvable"));
    }

    @Transactional
    public void supprimer(Long userId, Long documentId) {
        Document doc = trouver(userId, documentId);
        candidatureRepository.detacherCv(doc);
        candidatureRepository.detacherLettre(doc);
        contenuRepository.deleteById(doc.getId());
        documentRepository.delete(doc);
    }
}
