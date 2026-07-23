package br.com.iforce.praxis.results.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Roteiro revisado pelo entrevistador antes de ser registrado na trilha da tentativa. */
public record SaveInterviewGuideRequest(
        @Size(max = 30, message = "O roteiro aceita no máximo 30 perguntas.")
        List<@Valid Question> questions,

        @Size(max = 4000, message = "As anotações devem ter no máximo 4000 caracteres.")
        String interviewerNotes
) {

    public record Question(
            @NotBlank(message = "O identificador da pergunta é obrigatório.")
            @Size(max = 80, message = "O identificador deve ter no máximo 80 caracteres.")
            String id,

            @Size(max = 180, message = "A competência deve ter no máximo 180 caracteres.")
            String competency,

            @NotBlank(message = "O texto da pergunta é obrigatório.")
            @Size(max = 600, message = "A pergunta deve ter no máximo 600 caracteres.")
            String question,

            @NotBlank(message = "A origem da pergunta é obrigatória.")
            @Pattern(
                    regexp = "RULE|EVIDENCE|INTERVIEWER",
                    message = "A origem deve ser RULE, EVIDENCE ou INTERVIEWER."
            )
            String sourceType,

            @Size(max = 500, message = "A referência da evidência deve ter no máximo 500 caracteres.")
            String evidenceReference
    ) {
    }
}
