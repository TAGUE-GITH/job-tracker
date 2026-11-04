package com.jobtracker.document;

import com.jobtracker.candidature.CandidatureRepository;
import com.jobtracker.common.NotFoundException;
import com.jobtracker.user.UserRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final CandidatureRepository candidatureRepository;
    private final UserRepository userRepository;
    private final Path storageDir;

    public DocumentService(DocumentRepository documentRepository, CandidatureRepository candidatureRepository,
            UserRepository userRepository, @Value("${app.storage.dir}") String storageDir) {
        this.documentRepository = documentRepository;
        this.candidatureRepository = candidatureRepository;
        this.userRepository = userRepository;
        this.storageDir = Path.of(storageDir).toAbsolutePath().normalize();
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
        String nomOriginal = StringUtils.cleanPath(
                fichier.getOriginalFilename() == null ? "document" : fichier.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(nomOriginal);
        String nomStocke = UUID.randomUUID() + (extension == null ? "" : "." + extension);
        try {
            Files.createDirectories(storageDir);
            fichier.transferTo(storageDir.resolve(nomStocke));
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible d'enregistrer le fichier", e);
        }
        Document doc = new Document(nomOriginal, type, nomStocke, fichier.getContentType(),
                userRepository.getReferenceById(userId));
        return DocumentResponse.from(documentRepository.save(doc));
    }

    public Document trouver(Long userId, Long documentId) {
        return documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new NotFoundException("Document introuvable"));
    }

    public Resource fichier(Document doc) {
        Path path = storageDir.resolve(doc.getChemin()).normalize();
        if (!path.startsWith(storageDir) || !Files.exists(path)) {
            throw new NotFoundException("Fichier introuvable sur le disque");
        }
        return new PathResource(path);
    }

    @Transactional
    public void supprimer(Long userId, Long documentId) {
        Document doc = trouver(userId, documentId);
        candidatureRepository.detacherCv(doc);
        candidatureRepository.detacherLettre(doc);
        documentRepository.delete(doc);
        try {
            Files.deleteIfExists(storageDir.resolve(doc.getChemin()).normalize());
        } catch (IOException ignored) {
            // le document est supprime en base, un fichier orphelin n'est pas bloquant
        }
    }
}
