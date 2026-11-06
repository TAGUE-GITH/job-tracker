package com.jobtracker.ia;

import com.jobtracker.candidature.Candidature;
import com.jobtracker.candidature.CandidatureService;
import com.jobtracker.etape.Etape;
import com.jobtracker.etape.TypeEtape;
import com.jobtracker.ia.IaDtos.IaRequest;
import com.jobtracker.ia.IaDtos.IaResponse;
import com.jobtracker.ia.IaDtos.Message;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Assistant de preparation aux entretiens. Utilise n'importe quelle API compatible OpenAI
 * (Groq et OpenRouter ont des offres gratuites, Ollama tourne en local sans cle).
 */
@Service
public class IaService {

    private static final int MAX_TEXTE_OFFRE = 8000;

    private final CandidatureService candidatureService;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public IaService(CandidatureService candidatureService,
            @Value("${app.ia.base-url}") String baseUrl,
            @Value("${app.ia.api-key}") String apiKey,
            @Value("${app.ia.model}") String model) {
        this.candidatureService = candidatureService;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    record ChatRequest(String model, List<Message> messages, double temperature) {
    }

    record ChatResponse(List<Choice> choices) {
    }

    record Choice(Message message) {
    }

    @Transactional(readOnly = true)
    public IaResponse assister(Long userId, Long candidatureId, IaRequest req) {
        Candidature c = candidatureService.trouver(userId, candidatureId);
        TypeEtape etape = req.typeEtape() != null ? req.typeEtape() : TypeEtape.ENTRETIEN_TECHNIQUE;

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", promptSysteme(c, etape)));
        if (req.messages() == null || req.messages().isEmpty()) {
            messages.add(new Message("user", """
                    Prepare-moi pour cette etape. Donne :
                    1. Un resume en 3 points de ce que l'entreprise recherche.
                    2. Les 8 questions les plus probables, chacune avec une proposition de reponse \
                    structuree (methode STAR pour les questions comportementales).
                    3. 3 questions pertinentes que je peux poser au recruteur.
                    4. Les points techniques a reviser en priorite."""));
        } else {
            messages.addAll(req.messages());
        }
        return new IaResponse(appeler(messages));
    }

    private String promptSysteme(Candidature c, TypeEtape etape) {
        StringBuilder sb = new StringBuilder("""
                Tu es un coach carriere expert en recrutement tech. Tu aides un candidat a preparer \
                ses entretiens. Reponds en francais et tutoie le candidat, de facon concrete et structuree. \
                Mise en forme Markdown simple : titres (##, ###), listes a puces et numerotees, gras. \
                N'utilise jamais de tableaux ni de balises HTML (pas de <br>) : la reponse s'affiche \
                dans une colonne etroite. Pour chaque question, ecris la question en gras puis la \
                reponse conseillee en liste a puces. \
                Si le candidat te demande de simuler l'entretien, pose une seule question a la fois \
                puis donne un retour constructif sur sa reponse.
                """);
        sb.append("\nEntreprise : ").append(c.getNomEntreprise());
        sb.append("\nPoste : ").append(c.getPoste());
        sb.append("\nEtape a preparer : ").append(etape.name());
        if (c.getTexteOffre() != null) {
            String texte = c.getTexteOffre();
            if (texte.length() > MAX_TEXTE_OFFRE) {
                texte = texte.substring(0, MAX_TEXTE_OFFRE);
            }
            sb.append("\n\nTexte de l'offre :\n").append(texte);
        }
        List<Etape> avecNotes = c.getEtapes().stream().filter(e -> e.getNotes() != null).toList();
        if (!avecNotes.isEmpty()) {
            sb.append("\n\nHistorique et notes du candidat :");
            avecNotes.forEach(e -> sb.append("\n- ").append(e.getDate()).append(" ")
                    .append(e.getType().name()).append(" : ").append(e.getNotes()));
        }
        return sb.toString();
    }

    private String appeler(List<Message> messages) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IaIndisponibleException(
                    "IA non configuree : definis la variable AI_API_KEY (cle gratuite sur https://console.groq.com)");
        }
        try {
            ChatResponse res = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(new ChatRequest(model, messages, 0.7))
                    .retrieve()
                    .body(ChatResponse.class);
            if (res == null || res.choices() == null || res.choices().isEmpty()) {
                throw new IaIndisponibleException("Reponse vide du service d'IA");
            }
            return res.choices().get(0).message().content();
        } catch (RestClientException e) {
            throw new IaIndisponibleException("Le service d'IA ne repond pas : " + e.getMessage());
        }
    }
}
