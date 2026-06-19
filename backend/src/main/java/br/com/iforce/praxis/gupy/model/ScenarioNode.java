package br.com.iforce.praxis.gupy.model;

import br.com.iforce.praxis.shared.model.MediaType;

import java.util.List;

public record ScenarioNode(
        String id,
        int turnIndex,
        String speaker,
        String message,
        Integer timeLimitSeconds,
        String timeoutNextNodeId,
        String plainTextDescription,
        String audioDescriptionUrl,
        String mediaUrl,
        MediaType mediaType,
        List<ScenarioOption> options
) {

    /**
     * Construtor compatível com o formato anterior (sem mídia), usado por testes e fluxos legados.
     */
    public ScenarioNode(
            String id,
            int turnIndex,
            String speaker,
            String message,
            Integer timeLimitSeconds,
            List<ScenarioOption> options
    ) {
        this(id, turnIndex, speaker, message, timeLimitSeconds, null, null, null, null, null, options);
    }
}
