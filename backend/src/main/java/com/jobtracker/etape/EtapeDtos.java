package com.jobtracker.etape;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public final class EtapeDtos {

    private EtapeDtos() {
    }

    public record EtapeRequest(
            @NotNull(message = "Le type d'etape est obligatoire") TypeEtape type,
            @NotNull(message = "La date est obligatoire") LocalDate date,
            String notes) {
    }

    public record EtapeResponse(Long id, TypeEtape type, LocalDate date, String notes) {

        public static EtapeResponse from(Etape e) {
            return new EtapeResponse(e.getId(), e.getType(), e.getDate(), e.getNotes());
        }
    }
}
