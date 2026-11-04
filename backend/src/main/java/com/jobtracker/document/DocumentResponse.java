package com.jobtracker.document;

import java.time.LocalDate;

public record DocumentResponse(Long id, String nomFichier, TypeDocument type, LocalDate dateAjout) {

    public static DocumentResponse from(Document d) {
        return d == null ? null : new DocumentResponse(d.getId(), d.getNomFichier(), d.getType(), d.getDateAjout());
    }
}
