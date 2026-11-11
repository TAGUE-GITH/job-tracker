package com.jobtracker.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Photo facultative du candidat, ajoutee sur les CV generes s'il le demande. */
@Entity
@Table(name = "photo_profil")
public class PhotoProfil {

    @Id
    @Column(name = "user_id")
    private Long userId;

    // bytea : type binaire de PostgreSQL (aussi compris par H2)
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] donnees;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    protected PhotoProfil() {
    }

    public PhotoProfil(Long userId, byte[] donnees, String contentType) {
        this.userId = userId;
        this.donnees = donnees;
        this.contentType = contentType;
    }

    public Long getUserId() {
        return userId;
    }

    public byte[] getDonnees() {
        return donnees;
    }

    public String getContentType() {
        return contentType;
    }
}
