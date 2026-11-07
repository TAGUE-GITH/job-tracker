package com.jobtracker.user;

/** Regle commune a l'inscription, au changement et a la reinitialisation du mot de passe. */
public final class MotDePasse {

    /** Au moins 8 caracteres, dont une lettre et un chiffre. */
    public static final String REGLE = "^(?=.*\\p{L})(?=.*\\d).{8,100}$";
    public static final String MESSAGE = "8 caracteres minimum, avec au moins une lettre et un chiffre";

    private MotDePasse() {
    }
}
