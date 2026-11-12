package com.jobtracker.offre;

import java.time.Duration;

/**
 * Une source refuse durablement l'acces (cle refusee, API fermee, protection anti-robots) : inutile de
 * l'interroger a chaque recherche, elle est mise en pause pendant la duree indiquee.
 */
public class SourceBloqueeException extends OffresIndisponiblesException {

    private final transient Duration pause;

    public SourceBloqueeException(String message, Duration pause) {
        super(message);
        this.pause = pause;
    }

    public Duration pause() {
        return pause;
    }
}
