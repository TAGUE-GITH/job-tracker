package com.jobtracker.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;

/**
 * Contenu binaire d'un document, dans une table a part pour ne pas charger
 * les fichiers quand on liste simplement les documents.
 * Stocke en base car le disque des hebergeurs gratuits (Render) est efface a chaque deploiement.
 */
@Entity
public class DocumentContenu {

    public static final int TAILLE_MAX = 10 * 1024 * 1024;

    @Id
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    // bytea : type binaire de PostgreSQL (aussi compris par H2)
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] donnees;

    protected DocumentContenu() {
    }

    public DocumentContenu(Document document, byte[] donnees) {
        this.document = document;
        this.donnees = donnees;
    }

    public byte[] getDonnees() {
        return donnees;
    }
}
