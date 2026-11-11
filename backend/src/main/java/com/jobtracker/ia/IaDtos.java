package com.jobtracker.ia;

import com.jobtracker.etape.TypeEtape;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class IaDtos {

    private IaDtos() {
    }

    /** Message d'une conversation, au format des API compatibles OpenAI. */
    public record Message(
            @Pattern(regexp = "user|assistant") String role,
            @NotBlank String content) {
    }

    /**
     * Sans message : l'IA genere une preparation complete pour l'etape visee.
     * Avec messages : on poursuit la conversation (simulation d'entretien, questions...).
     */
    public record IaRequest(TypeEtape typeEtape, List<@Valid Message> messages) {
    }

    /** Question posee a ORVYNA sur l'application Postulyn (aide generale, hors candidature). */
    public record AideRequest(@NotEmpty List<@Valid Message> messages) {
    }

    /** cvId facultatif : par defaut, le CV rattache a la candidature. */
    public record AnalyseRequest(Long cvId) {
    }

    public record LettreIaRequest(Long cvId, @Size(max = 1000) String consignes) {
    }

    public record IaResponse(String reponse) {
    }
}
