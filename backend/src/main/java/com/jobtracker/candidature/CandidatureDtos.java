package com.jobtracker.candidature;

import com.jobtracker.document.DocumentResponse;
import com.jobtracker.etape.EtapeDtos.EtapeResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public final class CandidatureDtos {

    private CandidatureDtos() {
    }

    public record CandidatureRequest(
            @NotBlank(message = "Le nom de l'entreprise est obligatoire") String nomEntreprise,
            @NotBlank(message = "Le poste est obligatoire") String poste,
            @Size(max = 1000) String lienOffre,
            String texteOffre,
            @NotNull(message = "La date de candidature est obligatoire") LocalDate dateCandidature,
            Long cvId,
            Long lettreId,
            /** Seul ABANDONNEE est pris en compte : les autres statuts decoulent des etapes. */
            Boolean abandonnee) {
    }

    /** Lettre relue par le candidat ; les blocs d'en-tete (plusieurs lignes) sont facultatifs. */
    public record LettreRequest(
            @NotBlank(message = "La lettre est vide") @Size(max = 20000) String texte,
            @Size(max = 600) String expediteur,
            @Size(max = 600) String destinataire,
            @Size(max = 200) String lieuDate) {
    }

    public record CandidatureResponse(
            Long id,
            String nomEntreprise,
            String poste,
            String lienOffre,
            String texteOffre,
            LocalDate dateCandidature,
            StatutCandidature statut,
            DocumentResponse cv,
            DocumentResponse lettre,
            List<EtapeResponse> etapes) {

        public static CandidatureResponse from(Candidature c) {
            return new CandidatureResponse(c.getId(), c.getNomEntreprise(), c.getPoste(), c.getLienOffre(),
                    c.getTexteOffre(), c.getDateCandidature(), c.getStatut(),
                    DocumentResponse.from(c.getCv()), DocumentResponse.from(c.getLettre()),
                    c.getEtapes().stream().map(EtapeResponse::from).toList());
        }
    }
}
