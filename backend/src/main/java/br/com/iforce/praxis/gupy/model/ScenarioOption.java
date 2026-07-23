package br.com.iforce.praxis.gupy.model;

import br.com.iforce.praxis.shared.model.MediaType;


import java.util.Map;


public record ScenarioOption(
        String id,
        String text,
        String nextNodeId,
        Map<String, Integer> competencyScores,
        boolean critical,
        String auditNote,
        String plainTextDescription,
        String audioDescriptionUrl,
        String mediaUrl,
        MediaType mediaType,
        String mediaTranscript,
        String mediaCaptionsUrl,
        String mediaVersion
) {

    /**
     * Construtor compatível com o formato anterior (sem mídia), usado por testes e fluxos legados.
     */
    public ScenarioOption(
            String id,
            String text,
            String nextNodeId,
            Map<String, Integer> competencyScores,
            boolean critical,
            String auditNote
    ) {
        this(id, text, nextNodeId, competencyScores, critical, auditNote, null, null, null, null, null, null, null);
    }
}
