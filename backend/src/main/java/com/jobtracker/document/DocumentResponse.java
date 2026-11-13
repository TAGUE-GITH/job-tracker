package com.jobtracker.document;

import java.time.LocalDate;

/** genere : document cree par Postulyn (CV adapte, lettre) et non importe par l'utilisateur. */
public record DocumentResponse(Long id, String nomFichier, TypeDocument type, LocalDate dateAjout, boolean genere) {

    public static DocumentResponse from(Document d) {
        return d == null ? null
                : new DocumentResponse(d.getId(), d.getNomFichier(), d.getType(), d.getDateAjout(), d.isGenere());
    }
}
